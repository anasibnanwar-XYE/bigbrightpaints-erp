#!/usr/bin/env bash
set -euo pipefail

echo "WSL process:"
pgrep -af cli-proxy-api || true

echo ""
echo "Listening (8317):"
if command -v ss >/dev/null 2>&1; then
  ss -ltnp | grep ":8317" || true
fi

echo ""
echo "Log tail:"
tail -n 50 "$HOME/CLIProxyAPI/proxy.log" 2>/dev/null || true

echo ""
echo "HTTP test:"
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8317/ || true

