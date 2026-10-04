#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
free-proxies.py — скачивает публичные списки бесплатных прокси и проверяет их
на пригодность для Minecraft (SOCKS5 + реальный status-пинг вашего сервера).

Зависимостей нет — только стандартная библиотека Python 3.8+.

Примеры:
    # скачать свежие списки и проверить всё против своего сервера
    python free-proxies.py --target swarkfound.gomc.fun:25565

    # только Россия, максимум 300 проверок, 200 потоков
    python free-proxies.py --country RU --limit 300 --threads 200

    # проверить собственный файл со списком ip:port
    python free-proxies.py --input my-proxies.txt

Результат:
    proxies-mc.txt    — прокси, через которые сервер ответил на статус-пинг  (лучшие)
    proxies-tcp.txt   — туннель до сервера установился, но пинг не ответил
    proxies-report.csv — отчёт: адрес, страна, задержка, версия сервера, игроки
"""

import argparse
import concurrent.futures as futures
import csv
import json
import os
import re
import socket
import struct
import sys
import time
import urllib.request

UA = {"User-Agent": "Mozilla/5.0 (free-proxies checker)"}

# --- Источники списков -------------------------------------------------------
# Формат: ("имя", "обычный URL", "URL через GitHub API (резерв)").
# Резерв нужен, если raw.githubusercontent.com недоступен.
SOURCES = [
    ("monosans (с гео)",
     "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies.json",
     "https://api.github.com/repos/monosans/proxy-list/contents/proxies.json"),
    ("TheSpeedX",
     "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5.txt",
     "https://api.github.com/repos/TheSpeedX/PROXY-List/contents/socks5.txt"),
    ("hookzof",
     "https://raw.githubusercontent.com/hookzof/socks5_list/master/proxy.txt",
     "https://api.github.com/repos/hookzof/socks5_list/contents/proxy.txt"),
    ("roosterkid",
     "https://raw.githubusercontent.com/roosterkid/openproxylist/main/SOCKS5_RAW.txt",
     "https://api.github.com/repos/roosterkid/openproxylist/contents/SOCKS5_RAW.txt"),
    ("proxifly",
     "https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/protocols/socks5/data.txt",
     "https://api.github.com/repos/proxifly/free-proxy-list/contents/proxies/protocols/socks5/data.txt"),
]

# ip:port, с необязательной схемой (socks5://, socks4://, http://) и без авторизации
LINE_RE = re.compile(
    r"^(?:(?:socks5|socks4|socks5h|http|https)://)?"
    r"(\d{1,3}(?:\.\d{1,3}){3}):(\d{1,5})$"
)


def fetch(url, raw=True):
    """Скачать URL. raw=True — отдать как текст через Accept: raw (для GitHub API)."""
    hdrs = dict(UA)
    if raw and "api.github.com" in url:
        hdrs["Accept"] = "application/vnd.github.raw"
    req = urllib.request.Request(url, headers=hdrs)
    with urllib.request.urlopen(req, timeout=45) as r:
        return r.read().decode("utf-8", "replace")


def download_all(country=None, quiet=False):
    """Вернуть dict: 'ip:port' -> страна (или '')."""
    found = {}

    def add(host, port, cc=""):
        try:
            host = host.strip()
            port = int(port)
        except (ValueError, AttributeError):
            return
        if not (0 < port < 65536):
            return
        key = f"{host}:{port}"
        if key not in found or (cc and not found[key]):
            found[key] = cc or found.get(key, "")

    for name, url, api_url in SOURCES:
        text = None
        for u in (url, api_url):
            try:
                text = fetch(u)
                break
            except Exception as e:  # noqa: BLE001
                err = e
        if text is None:
            if not quiet:
                print(f"  [!] {name}: не скачался ({err})", file=sys.stderr)
            continue

        n_before = len(found)
        if text.lstrip().startswith("["):
            # JSON (monosans): есть протокол, страна, таймаут
            try:
                for e in json.loads(text):
                    if e.get("protocol") != "socks5" or e.get("username"):
                        continue
                    cc = ""
                    try:
                        cc = e["geolocation"]["country"]["iso_code"]
                    except Exception:  # noqa: BLE001
                        pass
                    add(e.get("host", ""), e.get("port", 0), cc)
            except Exception as e:  # noqa: BLE001
                if not quiet:
                    print(f"  [!] {name}: сломанный JSON ({e})", file=sys.stderr)
                continue
        else:
            for line in text.splitlines():
                m = LINE_RE.match(line.strip())
                if m:
                    add(m.group(1), m.group(2))

        if not quiet:
            print(f"  [+] {name}: +{len(found) - n_before} (всего {len(found)})")

    if country:
        cc_want = country.upper()
        have_geo = {k: v for k, v in found.items() if v}
        filtered = {k: v for k, v in have_geo.items() if v == cc_want}
        # если гео ни у чего нет — фильтровать нечем, отдаём всё
        if filtered or have_geo:
            found = filtered
        if not quiet:
            print(f"  [i] после фильтра по стране {cc_want}: {len(found)}")
    return found


# --- SOCKS5 ------------------------------------------------------------------
def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise OSError("соединение закрыто")
        buf += chunk
    return buf


def socks5_connect(sock, host, port):
    """Установить SOCKS5-туннель до host:port (без авторизации). True/False."""
    sock.sendall(b"\x05\x01\x00")
    if recv_exact(sock, 2) != b"\x05\x00":
        return False  # требует авторизацию или не SOCKS5
    try:
        ip = socket.gethostbyname(host)  # резолвим локально: так работает чаще
        req = b"\x05\x01\x00\x01" + socket.inet_aton(ip)
    except OSError:
        ip_bytes = host.encode()[:255]
        req = b"\x05\x01\x00\x03" + bytes([len(ip_bytes)]) + ip_bytes
    sock.sendall(req + struct.pack(">H", port))
    head = recv_exact(sock, 4)
    if head[0] != 5 or head[1] != 0:
        return False
    atyp = head[3]
    if atyp == 1:
        recv_exact(sock, 4 + 2)
    elif atyp == 3:
        recv_exact(sock, recv_exact(sock, 1)[0] + 2)
    elif atyp == 4:
        recv_exact(sock, 16 + 2)
    return True


# --- Minecraft status ping ---------------------------------------------------
def varint(n):
    n &= 0xFFFFFFFF
    out = b""
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out += bytes([b | 0x80])
        else:
            return out + bytes([b])


def read_varint(sock):
    num = 0
    for i in range(5):
        b = recv_exact(sock, 1)[0]
        num |= (b & 0x7F) << (7 * i)
        if not (b & 0x80):
            return num
    raise OSError("плохой varint")


def mc_status(sock, host, port, protocol):
    """Отправить handshake+status; вернуть dict с ответом сервера или None."""
    hb = host.encode()[:255]
    pkt = (b"\x00" + varint(protocol) + varint(len(hb)) + hb +
           struct.pack(">H", port) + varint(1))
    sock.sendall(varint(len(pkt)) + pkt)
    sock.sendall(b"\x01\x00")  # status request
    length = read_varint(sock)
    if length <= 0 or length > 1 << 20:
        return None
    data = recv_exact(sock, length)
    # packet id (varint) + string json
    i = 0
    while i < 5:
        b = data[i]
        i += 1
        if not (b & 0x80):
            break
    slen = 0
    for j in range(5):
        b = data[i]
        i += 1
        slen |= (b & 0x7F) << (7 * j)
        if not (b & 0x80):
            break
    return json.loads(data[i:i + slen].decode("utf-8", "replace"))


def check(proxy, target_host, target_port, protocol, timeout):
    """Проверить один прокси. Вернуть (ip:port, статус, мс, инфо)."""
    host, port = proxy.rsplit(":", 1)
    port = int(port)
    t0 = time.time()
    s = None
    try:
        s = socket.create_connection((host, port), timeout=timeout)
        s.settimeout(timeout)
        if not socks5_connect(s, target_host, target_port):
            return proxy, "bad-socks", None, None
        tunnel_ms = int((time.time() - t0) * 1000)
        try:
            st = mc_status(s, target_host, target_port, protocol)
        except Exception:  # noqa: BLE001
            return proxy, "tunnel", tunnel_ms, None
        ver = (st or {}).get("version", {}).get("name", "?")
        online = (st or {}).get("players", {}).get("online", "?")
        mx = (st or {}).get("players", {}).get("max", "?")
        return proxy, "mc", tunnel_ms, f"{ver}; {online}/{mx}"
    except Exception:  # noqa: BLE001
        return proxy, "dead", None, None
    finally:
        if s:
            try:
                s.close()
            except OSError:
                pass


def load_input(path):
    out = []
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            m = LINE_RE.match(line.strip())
            if m:
                out.append(f"{m.group(1)}:{m.group(2)}")
    return out


def main():
    ap = argparse.ArgumentParser(description="Скачать и проверить бесплатные SOCKS5-прокси для Minecraft")
    ap.add_argument("--target", default="swarkfound.gomc.fun:25565", help="хост:порт вашего сервера")
    ap.add_argument("--input", help="свой файл со списком ip:port (вместо скачивания)")
    ap.add_argument("--country", help="фильтр страны по гео, напр. RU (только для источника monosans)")
    ap.add_argument("--limit", type=int, default=0, help="проверить не больше N прокси")
    ap.add_argument("--max-pool", type=int, default=3000,
                    help="сколько кандидатов максимум брать из скачанных списков (по умолчанию 3000; "
                         "0 = все). Первыми идут самые свежие источники")
    ap.add_argument("--threads", type=int, default=150, help="потоков проверки (по умолчанию 150)")
    ap.add_argument("--timeout", type=float, default=6.0, help="таймаут на прокси, сек")
    ap.add_argument("--protocol", type=int, default=769, help="версия протокола MC для пинга")
    ap.add_argument("--out", default="proxies", help="префикс выходных файлов")
    ap.add_argument("--quiet", action="store_true")
    ap.add_argument("--insecure-ok", action="store_true",
                    help="не предупреждать про опасность бесплатных прокси")
    args = ap.parse_args()

    if not args.insecure_ok and not args.quiet:
        print("! Бесплатные прокси — публичные. Не вводите через них свои пароли,\n"
              "  не используйте Microsoft-аккаунты. Только offline-боты/тест.\n")

    host, _, port = args.target.partition(":")
    port = int(port or 25565)
    print(f"Цель: {host}:{port}\n")

    if args.input:
        candidates = load_input(args.input)
        print(f"Из файла: {len(candidates)} прокси")
    else:
        print("Скачиваю списки:")
        candidates = list(download_all(args.country, args.quiet).items())

    if not candidates and not args.input:
        print("Не удалось получить списки (нет доступа к github?).", file=sys.stderr)
        return 1

    if isinstance(candidates[0], tuple):  # [(ip:port, cc), ...]
        geo = dict(candidates)
        candidates = [k for k, _ in candidates]
    else:
        geo = {}

    if not args.input and args.max_pool and len(candidates) > args.max_pool:
        print(f"Беру первых {args.max_pool} из {len(candidates)} кандидатов (--max-pool), "
              f"остальные — по желанию через --max-pool 0")
        candidates = candidates[:args.max_pool]
    if args.limit:
        candidates = candidates[:args.limit]
    print(f"\nПроверяю {len(candidates)} прокси в {args.threads} потоков, таймаут {args.timeout:g}с...")

    ok_mc, ok_tcp, dead = [], [], 0
    t0 = time.time()
    with futures.ThreadPoolExecutor(max_workers=args.threads) as ex:
        jobs = [ex.submit(check, p, host, port, args.protocol, args.timeout) for p in candidates]
        for i, j in enumerate(futures.as_completed(jobs), 1):
            proxy, status, ms, info = j.result()
            if status == "mc":
                ok_mc.append((proxy, ms, info))
                print(f"  [MC]    {proxy:24} {ms:>5} мс  {info}  ({geo.get(proxy, '')})")
            elif status == "tunnel":
                ok_tcp.append((proxy, ms))
            else:
                dead += 1
            if i % 100 == 0:
                print(f"  ... {i}/{len(candidates)} (рабочих {len(ok_mc)} + {len(ok_tcp)} туннелей)")

    ok_mc.sort(key=lambda x: x[1] or 99999)
    ok_tcp.sort(key=lambda x: x[1] or 99999)

    f_mc = f"{args.out}-mc.txt"
    f_tcp = f"{args.out}-tcp.txt"
    with open(f_mc, "w", encoding="utf-8") as f:
        f.write("\n".join(p for p, _, _ in ok_mc) + ("\n" if ok_mc else ""))
    with open(f_tcp, "w", encoding="utf-8") as f:
        f.write("\n".join(p for p, _ in ok_tcp) + ("\n" if ok_tcp else ""))
    with open(f"{args.out}-report.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter=";")
        w.writerow(["proxy", "status", "latency_ms", "country", "server_info"])
        for p, ms, info in ok_mc:
            w.writerow([p, "mc-status-ok", ms, geo.get(p, ""), info])
        for p, ms in ok_tcp:
            w.writerow([p, "tunnel-ok", ms, geo.get(p, ""), ""])

    dt = time.time() - t0
    print(f"\nГотово за {dt:.0f}с. Мёртвых: {dead}")
    print(f"  Сервер ответил через прокси: {len(ok_mc)}  ->  {f_mc}")
    print(f"  Туннель без ответа сервера:  {len(ok_tcp)}  ->  {f_tcp}")
    print(f"  Отчёт: {args.out}-report.csv")
    if ok_mc:
        print("\nЛучшие 10:")
        for p, ms, info in ok_mc[:10]:
            print(f"  {p:24} {ms:>5} мс  {info}  {geo.get(p, '')}")
    print("\nИмпорт в SoulFire: Настройки прокси -> тип SOCKS5 -> вставить строки из файла.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
