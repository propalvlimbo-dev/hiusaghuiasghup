#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
proxy-parser.py — собирает SOCKS5-прокси со всех доступных источников (GitHub-списки,
публичные API, HTML-сайты), проверяет их настоящим SOCKS5-подключением к вашему
Minecraft-серверу и складывает рабочие в ОДИН файл для SoulFire.

Зависимостей нет — только стандартный Python 3.8+.

Быстрый старт:
    python proxy-parser.py --target swarkfound.gomc.fun:25565
    python proxy-parser.py --target swarkfound.gomc.fun:25565 --country RU --threads 300
    python proxy-parser.py --input my-list.txt --no-fetch      # проверить свой список

Что получится:
    socks5-working.txt       — рабочие SOCKS5 без авторизации   (ip:port)  ← импорт в SoulFire
    socks5-working-auth.txt  — рабочие с логином/паролем        (ip:port:user:pass)
    socks5-pool.txt          — весь собранный пул (до проверки)
    socks5-report.csv        — отчёт: статус, задержка, страна, версия сервера

Проверка честная: скрипт поднимает SOCKS5-туннель до вашего сервера и шлёт реальный
Minecraft status-пинг. Прокси, через который сервер ответил, годится для игры.
"""

import argparse
import concurrent.futures as futures
import csv
import gzip
import io
import json
import os
import random
import re
import socket
import struct
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")

# ---------------------------------------------------------------------------
# ИСТОЧНИКИ
# kind:
#   plain      — текст, строки вида ip:port / scheme://ip:port / ip:port:cc
#   monosans   — GitHub JSON с гео и протоколом
#   geonode    — JSON API с постраничной выдачей
#   proxyscrape— API v4 (text)
#   html       — HTML/JS-страница; адреса вытаскиваются универсальным regex'ом
# ---------------------------------------------------------------------------
SOURCES = [
    # --- GitHub-списки (обновляются автоматически) ---
    dict(name="monosans", kind="monosans",
         url="https://raw.githubusercontent.com/monosans/proxy-list/main/proxies.json"),
    dict(name="TheSpeedX", kind="plain",
         url="https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5.txt"),
    dict(name="hookzof", kind="plain",
         url="https://raw.githubusercontent.com/hookzof/socks5_list/master/proxy.txt"),
    dict(name="proxifly", kind="plain",
         url="https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/protocols/socks5/data.txt"),
    dict(name="roosterkid", kind="plain",
         url="https://raw.githubusercontent.com/roosterkid/openproxylist/main/SOCKS5_RAW.txt"),
    dict(name="ShiftyTR", kind="plain",
         url="https://raw.githubusercontent.com/ShiftyTR/Proxy-List/master/socks5.txt"),
    dict(name="hideip.me", kind="plain",
         url="https://raw.githubusercontent.com/zloi-user/hideip.me/main/socks5.txt"),
    dict(name="mmpx12", kind="plain",
         url="https://raw.githubusercontent.com/mmpx12/proxy-list/master/socks5.txt"),
    dict(name="ALIILAPRO", kind="plain",
         url="https://raw.githubusercontent.com/ALIILAPRO/Proxy/main/socks5.txt"),
    dict(name="prxchk", kind="plain",
         url="https://raw.githubusercontent.com/prxchk/proxy-list/main/socks5.txt"),

    # --- Публичные API ---
    dict(name="proxyscrape", kind="proxyscrape", pages=4,
         url="https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies"
             "&protocol=socks5&proxy_format=ipport&format=text&timeout=10000"),
    dict(name="proxy-list.download", kind="plain",
         url="https://www.proxy-list.download/api/v1/get?type=socks5"),
    dict(name="geonode", kind="geonode", pages=4,
         url="https://proxylist.geonode.com/api/proxy-list?limit=500&page={page}"
             "&sort_by=lastChecked&sort_type=desc&protocols=socks5"),
    dict(name="spys.me", kind="plain", url="https://spys.me/socks.txt"),

    # --- HTML-страницы (универсальный парсер) ---
    dict(name="socks-proxy.net", kind="html", url="https://www.socks-proxy.net/"),
    dict(name="proxy-list.download (html)", kind="html", url="https://www.proxy-list.download/SOCKS5"),
    dict(name="proxylistplus", kind="html", url="https://list.proxylistplus.com/SOCKS-List-1"),
    dict(name="openproxy.space", kind="html", url="https://openproxy.space/list/socks5"),
    dict(name="my-proxy", kind="html", url="https://www.my-proxy.com/free-socks-5-proxy.html"),
    dict(name="advanced.name", kind="html", url="https://advanced.name/freeproxy?is_socks5=1"),
    dict(name="proxydb", kind="html", url="https://proxydb.net/?protocol=socks5"),
]

IP_PORT_RE = re.compile(
    r"(?:(?:socks5|socks5h)://)?"
    r"(?:(?P<user>[^\s:/@]+):(?P<pw>[^\s:/@]+)@)?"
    r"(?P<ip>(?:\d{1,3}\.){3}\d{1,3})"
    r"[:\s\"'<>&nbsp;]+?(?P<port>\d{2,5})\b"
)
PAIR_RE = re.compile(
    r"^\s*(?P<ip>(?:\d{1,3}\.){3}\d{1,3})"
    r"(?:[:\s]+(?P<port>\d{2,5}))?(?:[:\s,;|]+(?P<cc>[A-Za-z]{2}))?\s*$",
    re.M,  # многострочный: ^/$ на каждой строке, иначе не сработает на списках
)


def valid_ip(ip):
    try:
        return all(0 <= int(p) <= 255 for p in ip.split("."))
    except ValueError:
        return False


def valid_port(p):
    try:
        return 1 <= int(p) <= 65535
    except (TypeError, ValueError):
        return False


def key_of(ip, port, user=None, pw=None):
    return f"{ip}:{port}" + (f":{user}:{pw}" if user else "")


# ---------------------------------------------------------------------------
# Загрузка
# ---------------------------------------------------------------------------
def api_fallback(url):
    """raw.githubusercontent.com/... -> api.github.com/repos/.../contents/..."""
    m = re.match(r"https://raw\.githubusercontent\.com/([^/]+)/([^/]+)/([^/]+)/(.+)", url)
    if not m:
        return None
    user, repo, _branch, path = m.groups()
    return f"https://api.github.com/repos/{user}/{repo}/contents/{path}"


def fetch(url, timeout=20):
    """Скачать URL; при обрыве — повторить и, для raw.githubusercontent, уйти на GitHub API."""
    fb = api_fallback(url)
    urls = [url] + ([fb] if fb else [])
    last = None
    for attempt, u in enumerate(urls + urls[:1]):  # url -> api -> ещё раз url
        hdrs = {"User-Agent": UA, "Accept": "*/*", "Accept-Language": "en-US,en;q=0.8"}
        if "api.github.com" in u:
            hdrs["Accept"] = "application/vnd.github.raw"
        try:
            req = urllib.request.Request(u, headers=hdrs)
            with urllib.request.urlopen(req, timeout=timeout) as r:
                raw = r.read()
            if raw[:2] == b"\x1f\x8b":
                raw = gzip.decompress(raw)
            return raw.decode("utf-8", "replace")
        except Exception as e:  # noqa: BLE001
            last = e
    raise last if last else RuntimeError("fetch failed")


# ---------------------------------------------------------------------------
# Парсинг
# ---------------------------------------------------------------------------
def extract(text, meta=None):
    """Вытащить все ip:port (и ip:port:user:pass) из произвольного текста."""
    meta = meta or {}
    out = {}
    for m in IP_PORT_RE.finditer(text):
        ip, port = m.group("ip"), m.group("port")
        if not (valid_ip(ip) and valid_port(port)):
            continue
        user, pw = m.group("user"), m.group("pw")
        # «user:pass@» ловим только если это похоже на авторизацию (не часть URL/HTML)
        if user and (len(user) > 32 or len(pw) > 32 or "@" in user):
            user = pw = None
        out[key_of(ip, port, user, pw)] = dict(meta, ip=ip, port=int(port), user=user, pw=pw)
    # строки вида «ip:port RU» — добавляем страну к уже найденным
    for m in PAIR_RE.finditer(text):
        ip, port, cc = m.group("ip"), m.group("port"), m.group("cc")
        if valid_ip(ip) and valid_port(port):
            key = key_of(ip, port)
            if key in out:
                if cc:
                    out[key]["cc"] = cc.upper()
            else:
                out[key] = dict(meta, ip=ip, port=int(port), cc=(cc or "").upper())
    return out


def extract_html(text, meta=None):
    """То же, но ещё и «табличный» формат: <td>1.2.3.4</td><td>8080</td>."""
    out = extract(text, meta)
    cell = re.findall(r">\s*((?:\d{1,3}\.){3}\d{1,3})\s*<[\s\S]{0,120}?>\s*(\d{2,5})\s*<", text)
    for ip, port in cell:
        if valid_ip(ip) and valid_port(port):
            out.setdefault(key_of(ip, port), dict(meta or {}, ip=ip, port=int(port)))
    return out


def parse_source(src, quiet=False, timeout=20):
    """Скачать один источник и вернуть {key: meta}."""
    name, kind = src["name"], src["kind"]
    pages = src.get("pages", 1)
    got, errors = {}, []

    for page in range(1, pages + 1):
        url = src["url"].format(page=page)
        if kind == "proxyscrape" and page > 1:
            url += f"&skip={(page - 1) * 100}"
        try:
            text = fetch(url, timeout)
        except Exception as e:  # noqa: BLE001
            errors.append(str(e)[:80])
            break

        if kind == "monosans":
            try:
                for e in json.loads(text):
                    if e.get("protocol") != "socks5":
                        continue
                    ip, port = e.get("host", ""), e.get("port", 0)
                    if not (valid_ip(ip) and valid_port(port)):
                        continue
                    cc = ""
                    try:
                        cc = e["geolocation"]["country"]["iso_code"]
                    except Exception:  # noqa: BLE001
                        pass
                    key = key_of(ip, port, e.get("username"), e.get("password"))
                    got[key] = dict(ip=ip, port=int(port), cc=cc,
                                    user=e.get("username"), pw=e.get("password"),
                                    source=name, ping=e.get("timeout"))
            except Exception as e:  # noqa: BLE001
                errors.append(f"json: {e}")
        elif kind == "geonode":
            try:
                for e in json.loads(text).get("data", []):
                    ip, port = e.get("ip"), e.get("port")
                    if valid_ip(ip or "") and valid_port(port):
                        got.setdefault(key_of(ip, port),
                                       dict(ip=ip, port=int(port), cc=e.get("country", ""),
                                            source=name))
            except Exception as e:  # noqa: BLE001
                errors.append(f"json: {e}")
        elif kind == "html":
            got.update(extract_html(text, {"source": name}))
        else:  # plain / proxyscrape
            for k, v in extract(text, {"source": name}).items():
                got.setdefault(k, v)

    if not quiet:
        note = f"  [!] {name}: {errors[0]}" if errors and not got else ""
        print(f"  [+] {name:24} +{len(got)}{note}")
    return got


def collect(sources, country=None, quiet=False, timeout=20):
    """Скачать все источники параллельно и слить в один пул."""
    pool, done = {}, 0
    with futures.ThreadPoolExecutor(max_workers=min(8, max(1, len(sources)))) as ex:
        jobs = {ex.submit(parse_source, s, True, timeout): s for s in sources}
        for j in futures.as_completed(jobs):
            src = jobs[j]
            done += 1
            try:
                got = j.result()
            except Exception as e:  # noqa: BLE001
                got = {}
                if not quiet:
                    print(f"  [!] {src['name']}: {str(e)[:70]}")
            for k, v in got.items():
                pool.setdefault(k, v)
            if not quiet:
                print(f"  [{done}/{len(sources)}] {src['name']:24} +{len(got):5}  (всего {len(pool)})")
    if country:
        cc = country.upper()
        before = len(pool)
        with_geo = sum(1 for v in pool.values() if v.get("cc"))
        if with_geo:  # гео есть — фильтруем, но не выбрасываем записи без гео
            pool = {k: v for k, v in pool.items() if not v.get("cc") or v.get("cc") == cc}
        if not quiet:
            print(f"  [i] страна {cc}: {before} -> {len(pool)} "
                  f"({'гео есть у ' + str(with_geo) if with_geo else 'гео нет, фильтр пропущен'})")
    return pool


# ---------------------------------------------------------------------------
# SOCKS5 + Minecraft
# ---------------------------------------------------------------------------
def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise OSError("closed")
        buf += chunk
    return buf


def socks5_connect(sock, host, port, user=None, pw=None):
    """Установить SOCKS5-туннель. Вернуть (True, "ok") или (False, причина отказа)."""
    if user:
        sock.sendall(b"\x05\x02\x00\x02")
        if recv_exact(sock, 2) != b"\x05\x02":
            return False, "auth-method-rejected"
        u, p = user.encode()[:255], pw.encode()[:255]
        sock.sendall(b"\x01" + bytes([len(u)]) + u + bytes([len(p)]) + p)
        if recv_exact(sock, 2)[1] != 0:
            return False, "auth-failed"
    else:
        sock.sendall(b"\x05\x01\x00")
        if recv_exact(sock, 2) != b"\x05\x00":
            return False, "needs-auth / not-socks5"
    try:
        ip = socket.gethostbyname(host)
        req = b"\x05\x01\x00\x01" + socket.inet_aton(ip)
    except OSError:
        h = host.encode()[:255]
        req = b"\x05\x01\x00\x03" + bytes([len(h)]) + h
    sock.sendall(req + struct.pack(">H", port))
    head = recv_exact(sock, 4)
    if head[0] != 5:
        return False, "not-socks5"
    atyp = head[3]
    if atyp == 1:
        recv_exact(sock, 6)
    elif atyp == 3:
        recv_exact(sock, recv_exact(sock, 1)[0] + 2)
    elif atyp == 4:
        recv_exact(sock, 18)
    if head[1] != 0:
        # код ответа SOCKS5: 3 сеть недоступна, 4 хост недоступен, 5 отказ, 6 TTL, 7 команда
        return False, {3: "net-unreachable", 4: "host-unreachable", 5: "refused",
                       6: "ttl-expired", 7: "cmd-not-supported",
                       8: "atyp-not-supported"}.get(head[1], f"rep-code-{head[1]}")
    return True, "ok"


def varint(n):
    n &= 0xFFFFFFFF
    out = b""
    while True:
        b = n & 0x7F
        n >>= 7
        out += bytes([b | 0x80]) if n else bytes([b])
        if not n:
            return out


def read_varint(sock):
    num = 0
    for i in range(5):
        b = recv_exact(sock, 1)[0]
        num |= (b & 0x7F) << (7 * i)
        if not (b & 0x80):
            return num
    raise OSError("bad varint")


def mc_status(sock, host, port, protocol):
    h = host.encode()[:255]
    pkt = b"\x00" + varint(protocol) + varint(len(h)) + h + struct.pack(">H", port) + varint(1)
    sock.sendall(varint(len(pkt)) + pkt)
    sock.sendall(b"\x01\x00")
    length = read_varint(sock)
    if length <= 0 or length > 1 << 20:
        return None
    data = recv_exact(sock, length)
    i = 0
    while i < 5 and data[i] & 0x80:
        i += 1
    i += 1
    slen = 0
    for j in range(5):
        b = data[i]
        i += 1
        slen |= (b & 0x7F) << (7 * j)
        if not (b & 0x80):
            break
    return json.loads(data[i:i + slen].decode("utf-8", "replace"))


def check(meta, target_host, target_port, protocol, timeout, mc_ping=True):
    """Проверить один прокси. Вернуть (key, status, ms, info)."""
    ip, port = meta["ip"], meta["port"]
    t0 = time.time()
    s = None
    try:
        s = socket.create_connection((ip, port), timeout=timeout)
        s.settimeout(timeout)
        ok, reason = socks5_connect(s, target_host, target_port, meta.get("user"), meta.get("pw"))
        if not ok:
            return key_of(ip, port, meta.get("user"), meta.get("pw")), "no-tunnel:" + reason, None, None
        ms = int((time.time() - t0) * 1000)
        if not mc_ping:
            return key_of(ip, port, meta.get("user"), meta.get("pw")), "tunnel", ms, None
        try:
            st = mc_status(s, target_host, target_port, protocol)
        except Exception:  # noqa: BLE001
            return key_of(ip, port, meta.get("user"), meta.get("pw")), "tunnel", ms, None
        v = (st or {}).get("version", {}).get("name", "?")
        p = (st or {}).get("players", {})
        return (key_of(ip, port, meta.get("user"), meta.get("pw")), "mc", ms,
                f"{v}; {p.get('online', '?')}/{p.get('max', '?')}")
    except socket.timeout:
        return key_of(ip, port, meta.get("user"), meta.get("pw")), "no-tunnel:timeout", None, None
    except Exception as e:  # noqa: BLE001
        return (key_of(ip, port, meta.get("user"), meta.get("pw")),
                "dead:" + type(e).__name__, None, None)
    finally:
        if s:
            try:
                s.close()
            except OSError:
                pass


# ---------------------------------------------------------------------------
def load_file(path):
    out = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for k, v in extract(f.read()).items():
            out[k] = v
    return out


def main():
    ap = argparse.ArgumentParser(
        description="Собрать и проверить бесплатные SOCKS5-прокси для SoulFire")
    ap.add_argument("--target", default="swarkfound.gomc.fun:25565", help="хост:порт своего сервера")
    ap.add_argument("--input", action="append", default=[], help="свой файл(ы) со списком прокси")
    ap.add_argument("--no-fetch", action="store_true", help="не скачивать источники, только --input")
    ap.add_argument("--sources", help="список имён источников через запятую (по умолчанию все)")
    ap.add_argument("--country", help="фильтр страны по гео (напр. RU) — где гео известно")
    ap.add_argument("--limit", type=int, default=0, help="проверить не больше N прокси")
    ap.add_argument("--threads", type=int, default=250, help="потоков проверки (по умолчанию 250)")
    ap.add_argument("--timeout", type=float, default=6.0, help="таймаут на прокси, сек")
    ap.add_argument("--fetch-timeout", type=float, default=20.0, help="таймаут на скачивание источника, сек")
    ap.add_argument("--protocol", type=int, default=769, help="версия протокола MC для пинга")
    ap.add_argument("--tcp-only", action="store_true", help="не слать MC-пинг, хватит туннеля")
    ap.add_argument("--top", type=int, default=0, help="оставить только N лучших по задержке")
    ap.add_argument("--merge", action="store_true",
                    help="дописать результаты к существующим файлам, а не перезаписать")
    ap.add_argument("--out-dir", default=".", help="куда складывать файлы")
    ap.add_argument("--shuffle", action="store_true", help="перемешать пул перед проверкой")
    ap.add_argument("--quiet", action="store_true")
    args = ap.parse_args()

    host, _, port = args.target.partition(":")
    port = int(port or 25565)
    os.makedirs(args.out_dir, exist_ok=True)
    f_work = os.path.join(args.out_dir, "socks5-working.txt")
    f_auth = os.path.join(args.out_dir, "socks5-working-auth.txt")
    f_pool = os.path.join(args.out_dir, "socks5-pool.txt")
    f_rep = os.path.join(args.out_dir, "socks5-report.csv")

    if not args.quiet:
        print("=" * 72)
        print(" СБОР SOCKS5-ПРОКСИ ДЛЯ SOULFIRE")
        print(f" Цель проверки: {host}:{port}")
        print("! Публичные прокси видят ваш трафик. Только offline-боты, без своих паролей!")
        print("=" * 72)

    # 1. Сбор
    pool = {}
    if not args.no_fetch:
        srcs = SOURCES
        if args.sources:
            want = {s.strip().lower() for s in args.sources.split(",")}
            srcs = [s for s in SOURCES if s["name"].lower() in want]
        print(f"\nИсточники ({len(srcs)}):")
        pool = collect(srcs, args.country, args.quiet, args.fetch_timeout)
    for path in args.input:
        got = load_file(path)
        if not args.quiet:
            print(f"  [+] файл {path:24} +{len(got)}")
        pool.update(got)
    # уже проверенные ранее — не перепроверяем при --merge
    known = {}
    if args.merge:
        for f in (f_work, f_auth):
            if os.path.exists(f):
                known.update(load_file(f))
        for k in known:
            pool.pop(k, None)

    # сохранить пул
    with open(f_pool, "w", encoding="utf-8") as f:
        f.write("\n".join(sorted(pool)) + ("\n" if pool else ""))

    items = list(pool.items())
    if args.shuffle:
        random.shuffle(items)
    if not items:
        print("\nПул пуст: источники недоступны и --input не задан.", file=sys.stderr)
        return 1
    print(f"\nВ пуле {len(items)} прокси (сохранён в {os.path.basename(f_pool)}).")
    if args.limit:
        items = items[:args.limit]
    print(f"Проверяю {len(items)} в {args.threads} потоков, таймаут {args.timeout:g}с"
          f"{' (только TCP)' if args.tcp_only else ' + MC-пинг'}...")

    # 2. Проверка
    from collections import Counter
    ok_mc, ok_tcp, fails = [], [], Counter()
    t0 = time.time()
    with futures.ThreadPoolExecutor(max_workers=args.threads) as ex:
        jobs = {ex.submit(check, m, host, port, args.protocol, args.timeout, not args.tcp_only): k
                for k, m in items}
        done = 0
        for j in futures.as_completed(jobs):
            key, status, ms, info = j.result()
            done += 1
            m = pool.get(key, {})
            row = (key, ms, m.get("cc", ""), m.get("source", ""), info or "")
            if status == "mc":
                ok_mc.append(row)
                if not args.quiet:
                    print(f"  [MC]    {key:28} {ms:>5} мс  {info}  {row[2]}")
            elif status == "tunnel":
                ok_tcp.append(row)
            else:
                fails[status] += 1
            if not args.quiet and done % 200 == 0:
                print(f"  ... {done}/{len(items)}  рабочих: {len(ok_mc)} (MC) + {len(ok_tcp)} (TCP)")

    ok_mc.sort(key=lambda r: r[1] or 99999)
    ok_tcp.sort(key=lambda r: r[1] or 99999)
    if args.top:
        ok_mc, ok_tcp = ok_mc[:args.top], ok_tcp[:args.top]

    # 3. Один файл для SoulFire
    def dump(path, rows, auth=False):
        lines = [r[0] for r in rows]
        if args.merge and os.path.exists(path):
            old = [l.strip() for l in open(path, encoding="utf-8") if l.strip()]
            lines = list(dict.fromkeys(old + lines))
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + ("\n" if lines else ""))
        return len(lines)

    mc_noauth = [r for r in ok_mc if r[0].count(":") == 1]
    mc_auth = [r for r in ok_mc if r[0].count(":") == 3]
    tcp_noauth = [r for r in ok_tcp if r[0].count(":") == 1]
    n1 = dump(f_work, mc_noauth)
    n2 = dump(f_auth, mc_auth)
    tcp_file = os.path.join(args.out_dir, "socks5-tunnel-only.txt")
    n3 = dump(tcp_file, tcp_noauth)

    with open(f_rep, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter=";")
        w.writerow(["proxy", "status", "latency_ms", "country", "source", "server_info"])
        for rows, status in ((ok_mc, "mc-status-ok"), (ok_tcp, "tunnel-ok")):
            for key, ms, cc, src, info in rows:
                w.writerow([key, status, ms, cc, src, info])

    dt = time.time() - t0
    print("\n" + "=" * 72)
    print(f" ГОТОВО за {dt:.0f}с")
    print(f"  проверено: {len(items)}   не прошло: {sum(fails.values())}")
    for why, cnt in fails.most_common(6):
        hint = ""
        if why.startswith("no-tunnel:refused"):
            hint = "  <- цель (сервер/его защита) отказала"
        elif why.startswith("no-tunnel:timeout"):
            hint = "  <- прокси не смог достучаться до цели"
        elif why.startswith("no-tunnel:auth"):
            hint = "  <- неверный логин/пароль прокси"
        elif why.startswith("dead:"):
            hint = "  <- сам прокси недоступен"
        print(f"      {why:26} {cnt:6}{hint}")
    print(f"  сервер ответил через прокси (MC): {len(ok_mc)}")
    print(f"  туннель без ответа сервера:       {len(ok_tcp)}")
    print("-" * 72)
    print(f"  ФАЙЛ ДЛЯ SOULFIRE: {f_work}  ({n1} без авторизации)")
    if mc_auth:
        print(f"  с логином/паролем: {f_auth}  ({n2})")
    if n3:
        print(f"  запасной список:   {tcp_file}  ({n3})")
    print(f"  отчёт:             {f_rep}")
    print("-" * 72)
    if ok_mc:
        print(" Лучшие 10:")
        for key, ms, cc, src, info in ok_mc[:10]:
            print(f"  {key:28} {ms:>5} мс  {info:24} {cc} {src}")
    print(" Импорт: SoulFire -> Настройки прокси -> тип SOCKS5 -> вставить строки из файла,")
    print("         Proxy check address = адрес СВОЕГО сервера.")
    print("=" * 72)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\nПрервано пользователем.", file=sys.stderr)
        sys.exit(130)
