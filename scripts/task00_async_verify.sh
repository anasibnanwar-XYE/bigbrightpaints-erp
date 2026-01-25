#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG="/tmp/task00-verify.log"
PID="/tmp/task00-verify.pid"
EXIT="/tmp/task00-verify.exit"

rm -f "$EXIT"
printf "[task00] launcher %s\n" "$(date -Is)" > "$LOG"

setsid bash -lc "set +e; echo \"[task00] verify start $(date -Is)\"; cd \"$ROOT_DIR/erp-domain\" && mvn -B -ntp verify; status=\$?; echo \"[task00] verify exit \$status $(date -Is)\"; echo \$status > \"$EXIT\"" >> "$LOG" 2>&1 < /dev/null & echo $! > "$PID"

echo "started verify pid $(cat "$PID")"
