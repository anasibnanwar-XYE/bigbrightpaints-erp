#!/usr/bin/env bash
set -euo pipefail

LOG="/tmp/task00-verify.log"
PID_FILE="/tmp/task00-verify.pid"
EXIT="/tmp/task00-verify.exit"

if [[ -f "$PID_FILE" ]]; then
  pid="$(cat "$PID_FILE")"
  if ps -p "$pid" >/dev/null 2>&1; then
    echo "VERIFY RUNNING (pid $pid)"
  else
    echo "VERIFY FINISHED (pid $pid)"
  fi
else
  echo "VERIFY PID file missing ($PID_FILE)"
fi

if [[ -f "$EXIT" ]]; then
  echo "EXIT CODE: $(cat "$EXIT")"
else
  echo "EXIT CODE: (not yet written)"
fi

if [[ -f "$LOG" ]]; then
  echo "LOG (last 120 lines):"
  tail -n 120 "$LOG"
else
  echo "LOG missing ($LOG)"
fi
