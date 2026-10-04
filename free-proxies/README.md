# Бесплатные прокси: списки и чем их проверять

⚠️ **Это публичные прокси.** Их одновременно используют сотни людей. Через них **нельзя**:
вводить пароли, заходить в Microsoft-аккаунты, оплачивать что-либо. Только offline-боты и тесты.
Часть публичных прокси — «медовые ловушки», которые пишут весь трафик.

⚠️ **Живучесть — минуты-часы.** Файлы ниже — снимок на **2026-10-04**, в них гарантированно
много мёртвых адресов. Поэтому главное здесь не список, а скрипт проверки.

## Что лежит в папке

| Файл | Что это | Как использовать |
|---|---|---|
| `socks5-RU.txt` | 51 российский SOCKS5, отсортированы по свежести проверки источника | ставить первыми: наименьший пинг до РФ-сервера |
| `socks5-fresh-150.txt` | 150 недавно проверенных SOCKS5 (любые страны) | смешанный пул |
| `socks5-big-1500.txt` | 1500 адресов из четырёх источников | когда нужно много — но и мусора больше |

Формат — `ip:port`, по одной строке. Это ровно то, что ждёт **Настройки прокси → импорт**
в SoulFire (тип выбрать **SOCKS5**).

## Источники свежих списков (обновляются каждые 5–60 минут)

| Источник | Что даёт | Ссылка |
|---|---|---|
| **monosans/proxy-list** | проверенные SOCKS5 + гео + ASN, обновление ~5 мин | `https://raw.githubusercontent.com/monosans/proxy-list/main/proxies.json` |
| **TheSpeedX/PROXY-List** | большие списки socks4/socks5/http | `https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5.txt` |
| **hookzof/socks5_list** | крупный агрегат SOCKS5 | `https://raw.githubusercontent.com/hookzof/socks5_list/master/proxy.txt` |
| **proxifly/free-proxy-list** | SOCKS5 в формате `socks5://ip:port` | `https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/protocols/socks5/data.txt` |
| **roosterkid/openproxylist** | короткий свежий список SOCKS5 | `https://raw.githubusercontent.com/roosterkid/openproxylist/main/SOCKS5_RAW.txt` |
| **ProxyScrape API** | JSON/текст, есть фильтр по протоколу и стране | `https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies&protocol=socks5&proxy_format=ipport&format=text` |

## Парсер прокси (главный инструмент)

`scripts/proxy-parser.py` — собирает SOCKS5 из **21 источника** (GitHub-списки, публичные API,
HTML-сайты), проверяет каждый настоящим SOCKS5-туннелем до вашего сервера с реальным MC-пингом
и складывает рабочие в **один файл**:

```bash
# всё сразу: сбор -> проверка -> один файл рабочих
python scripts/proxy-parser.py --target swarkfound.gomc.fun:25565

# только Россия, 300 проверок, 300 потоков
python scripts/proxy-parser.py --target swarkfound.gomc.fun:25565 --country RU --limit 300 --threads 300

# свой список, без скачивания источников
python scripts/proxy-parser.py --input free-proxies/socks5-RU.txt --no-fetch

# дописать к прошлым результатам и оставить только 50 лучших
python scripts/proxy-parser.py --merge --top 50
```

| Выход | Что внутри |
|---|---|
| **socks5-working.txt** | ✅ рабочие SOCKS5 без авторизации, отсортированы по задержке — **это и есть файл для SoulFire** |
| socks5-working-auth.txt | рабочие с логином/паролем (`ip:port:user:pass`) |
| socks5-tunnel-only.txt | туннель поднялся, но сервер на пинг не ответил |
| socks5-pool.txt | весь собранный пул до проверки |
| socks5-report.csv | отчёт: статус, задержка, страна, источник, версия сервера |

Полезные ключи: `--tcp-only` (не слать MC-пинг), `--shuffle`, `--top N`, `--sources monosans,geonode`,
`--fetch-timeout 30`, `--out-dir`. Зависимостей нет — только Python 3.8+.

Лёгкая версия без HTML-источников и на 5 источников — `scripts/free-proxies.py`.

Альтернатива без Python: импортировать список в SoulFire, в **Настройки прокси → Proxy check address**
вписать адрес своего сервера и нажать проверку — мёртвые адреса удалить из списка.
