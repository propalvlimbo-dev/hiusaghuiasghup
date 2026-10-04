#!/usr/bin/env bash
#
# stress-stages.sh — поэтапный запуск «заливных» ботов по своему Minecraft-серверу.
#
# Назначение: провести ступенчатый тест входа (10 -> 25 -> 50 -> 100 ботов),
# зафиксировать маркеры ступеней в CSV, чтобы потом сопоставить их с MSPT/TPS и
# профилем Spark. Скрипт намеренно НЕ решает капчу: для ступеней с капчей нужен
# SoulFire, а здесь предполагается, что защита тестового сервера пропускает
# машину-генератор (permission/whitelist/ipset) — см. SOULFIRE-ПЛАН-СТРЕСС-ТЕСТА.md.
#
# ВНИМАНИЕ: запускать только по серверам, которые вам принадлежат или на тест
# которых есть явное разрешение. На чужом сервере это атака.
#
# Поддерживаемые инструменты:
#   mc-bots      — java -jar mc-bots.jar -s host:port -c N [-t SOCKS5 -l proxies.txt]
#   rust-mc-bot  — ./rust-mc-bot host:port N [threads]
#
# Примеры:
#   ./stress-stages.sh --tool mc-bots --jar ~/mc-bots-1.2.17.jar \
#       --host 127.0.0.1 --port 25565 --stages "10 25 50 100" --hold 600
#
#   ./stress-stages.sh --tool rust-mc-bot --bin ./rust-mc-bot \
#       --host 127.0.0.1 --port 25565 --stages "100 250 500" --hold 300 --yes
#
set -euo pipefail

HOST=""; PORT="25565"
TOOL="mc-bots"
JAR=""; BIN=""
STAGES="10 25 50 100"
HOLD=600
COOLDOWN=60
OUT="stress-$(date +%Y%m%d-%H%M%S).csv"
LOGDIR="stress-logs-$(date +%Y%m%d-%H%M%S)"
PROXIES=""; PROXY_TYPE="SOCKS5"
DELAY_MIN=2000; DELAY_MAX=3000
ASSUME_YES=0

usage() {
  sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="${2:-}"; shift 2 ;;
    --port) PORT="${2:-}"; shift 2 ;;
    --tool) TOOL="${2:-}"; shift 2 ;;
    --jar) JAR="${2:-}"; shift 2 ;;
    --bin) BIN="${2:-}"; shift 2 ;;
    --stages) STAGES="${2:-}"; shift 2 ;;
    --hold) HOLD="${2:-}"; shift 2 ;;
    --cooldown) COOLDOWN="${2:-}"; shift 2 ;;
    --out) OUT="${2:-}"; shift 2 ;;
    --proxies) PROXIES="${2:-}"; shift 2 ;;
    --proxy-type) PROXY_TYPE="${2:-}"; shift 2 ;;
    --delay) DELAY_MIN="${2:-}"; DELAY_MAX="${3:-$2}"; shift 3 ;;
    --yes) ASSUME_YES=1; shift ;;
    -h|--help) usage ;;
    *) echo "Неизвестный аргумент: $1" >&2; exit 2 ;;
  esac
done

die() { echo "Ошибка: $*" >&2; exit 1; }

[[ -n "$HOST" ]] || die "нужен --host"
[[ "$TOOL" == "mc-bots" || "$TOOL" == "rust-mc-bot" ]] || die "--tool: mc-bots | rust-mc-bot"
if [[ "$TOOL" == "mc-bots" ]]; then
  [[ -n "$JAR" && -f "$JAR" ]] || die "для mc-bots нужен --jar <путь к mc-bots.jar>"
  command -v java >/dev/null || die "java не найдена в PATH"
else
  [[ -n "$BIN" && -x "$BIN" ]] || die "для rust-mc-bot нужен --bin <исполняемый файл>"
fi
if [[ -n "$PROXIES" && ! -f "$PROXIES" ]]; then die "файл прокси не найден: $PROXIES"; fi

echo "=== Поэтапный стресс-тест ==="
echo "Цель:        $HOST:$PORT"
echo "Инструмент:  $TOOL"
echo "Ступени:     $STAGES  (выдержка ${HOLD}s, пауза ${COOLDOWN}s)"
echo "CSV:         $OUT"
echo "Логи:        $LOGDIR/"
echo
echo "Помните: тест только на своём сервере (или с письменным разрешением)."
echo "Убедитесь, что капча/анти-бот пропускает эту машину (bypass), иначе ступени"
echo "с большим числом ботов будут молча упираться в защиту, а не в сервер."
echo
if [[ "$ASSUME_YES" -ne 1 ]]; then
  read -r -p "Написать ровно 'yes' для продолжения: " ans
  [[ "$ans" == "yes" ]] || { echo "Отменено."; exit 1; }
fi

mkdir -p "$LOGDIR"
printf 'stage_started,stage_ended,stage,requested_bots,tool,hold_seconds,exit_code,log_file\n' > "$OUT"

# run_stage вызывается в отдельном bash (через timeout), поэтому нужные
# переменные должны быть экспортированы в окружение.
export HOST PORT TOOL JAR BIN PROXIES PROXY_TYPE DELAY_MIN DELAY_MAX

run_stage() {
  local n="$1" log="$2"
  if [[ "$TOOL" == "mc-bots" ]]; then
    local args=(-jar "$JAR" -s "$HOST:$PORT" -c "$n" -d "$DELAY_MIN" "$DELAY_MAX" -n)
    if [[ -n "$PROXIES" ]]; then args+=(-t "$PROXY_TYPE" -l "$PROXIES"); fi
    java "${args[@]}"
  else
    "$BIN" "$HOST:$PORT" "$n"
  fi
}

for n in $STAGES; do
  ts_start="$(date -Is)"
  log="$LOGDIR/stage-${n}.log"
  echo "[$(date +%H:%M:%S)] Ступень: ${n} ботов, выдержка ${HOLD}s -> $log"

  set +e
  timeout --signal=INT --kill-after=20s "${HOLD}s" bash -c "$(declare -f run_stage); run_stage $n '$log'" > "$log" 2>&1
  code=$?
  set -e

  ts_end="$(date -Is)"
  printf '%s,%s,%s,%s,%s,%s,%s,%s\n' \
    "$ts_start" "$ts_end" "$n" "$n" "$TOOL" "$HOLD" "$code" "$log" >> "$OUT"

  if [[ "$code" -eq 124 || "$code" -eq 137 ]]; then
    echo "  выдержка завершена, боты остановлены"
  elif [[ "$code" -ne 0 ]]; then
    echo "  ВНИМАНИЕ: инструмент завершился с кодом $code — смотрите $log"
    echo "  (частые причины: нужен online-mode=false, несовпадение версии протокола,"
    echo "   лимит max-players, кик анти-ботом, нехватка RAM у генератора)"
  else
    echo "  инструмент завершился сам (возможно, все боты были кикнуты — проверьте $log)"
  fi

  if [[ "$n" != "$(echo "$STAGES" | awk '{print $NF}')" ]]; then
    echo "  пауза ${COOLDOWN}s для наблюдения восстановления MSPT/TPS..."
    sleep "$COOLDOWN"
  fi
done

echo
echo "Готово. Сводка: $OUT"
echo "Колонки: stage_started, stage_ended, stage, requested_bots, tool, hold_seconds, exit_code, log_file"
echo
echo "Дальше:"
echo "  1) Сверьте маркеры ступеней с графиком MSPT/TPS и логами сервера."
echo "  2) Запустите /spark profiler start на 2 минуты внутри интересной ступени."
echo "  3) Запишите причины киков из логов сервера и из $LOGDIR/."
echo "  4) Повторите ступень, если нужно отличить всплеск от устойчивой деградации."
