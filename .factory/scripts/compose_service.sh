#!/usr/bin/env bash
set -euo pipefail
if [ "$#" -ne 2 ]; then
  echo "usage: compose_service.sh <service> <up|stop>" >&2
  exit 1
fi
ROOT="$(git rev-parse --show-toplevel)"
SERVICE="$1"
ACTION="$2"
cd "$ROOT"
case "$ACTION" in
  up)
    bash "$ROOT/.factory/init.sh" >/dev/null
    docker compose up -d "$SERVICE"
    ;;
  stop)
    docker compose stop "$SERVICE"
    ;;
  *)
    echo "unsupported action: $ACTION" >&2
    exit 1
    ;;
esac
