#!/usr/bin/env bash
set -euo pipefail

pkill -f cli-proxy-api 2>/dev/null || true
cd "$HOME/CLIProxyAPI"
nohup ./cli-proxy-api --config config.yaml > proxy.log 2>&1 &
sleep 0.6
tail -n 20 "$HOME/CLIProxyAPI/proxy.log" || true
echo ""
echo "CLIProxyAPI started on http://127.0.0.1:8317"
echo "Quick test:"
curl -sS http://127.0.0.1:8317/v1/responses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dummy" \
  -d '{"model":"gpt-5.2(xhigh)","input":"Reply with exactly: OK"}' | head -c 200
echo ""
