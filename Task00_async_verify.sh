#!/usr/bin/env bash
set -euo pipefail

LOG="${1:-/tmp/task00-verify.log}"
PID="${2:-/tmp/task00-verify.pid}"

nohup bash -lc 'cd erp-domain && mvn -B -ntp verify' > "$LOG" 2>&1 &
echo $! > "$PID"

echo "Started verify:"
echo "  PID: $(cat "$PID")"
echo "  LOG: $LOG"
     #!/usr/bin/env bash
     set -euo pipefail

     PID="${1:-/tmp/task00-verify.pid}"
     LOG="${2:-/tmp/task00-verify.log}"

     if [[ ! -f "$PID" ]]; then
       echo "No PID file at $PID"
       exit 1
     fi

     if ps -p "$(cat "$PID")" >/dev/null; then
       echo "VERIFY RUNNING (PID $(cat "$PID"))"
     else
       echo "VERIFY FINISHED (PID $(cat "$PID"))"
     fi

     tail -n 200 "$LOG"
