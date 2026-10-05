# elytrix-bots — Rust-демон ботов

Отдельный быстрый движок ботов (скорость BotMark), управляется из клиента Elytrix
по HTTP JSON на `127.0.0.1:25570` (гибкость управления как у SoulFire).

## Сборка (Windows)

1. Поставь Rust: https://rustup.rs (один клик).
2. В папке `elytrix-bots`:
   ```
   cargo build --release
   ```
3. Скопируй `target\release\elytrix-bots.exe` куда удобно, например `C:\tools\elytrix-bots.exe`.
4. В клиенте: Боты → «Путь к rust-ботам» → этот путь. Если файла нет — клиент
   автоматически использует встроенный Java-движок.

## API

- `GET /status` — `{running, alive, total, bots:[{name,status}]}`
- `POST /start` — JSON настроек (host, port, count, delay_ms, timeout_ms, prefix,
  spam, spam_message, rotation, swing, movement, auto_reg, auto_login, password)
- `POST /stop`

## Протокол

Раскладки пакетов 26.2 (protocol 776); номер протокола сервера узнаётся сам через
status-ping. Боты — offline-режим (как BotMark): сервер должен быть с
`online-mode=false`. Авторег/автовход шлют `/register` и `/login`.
