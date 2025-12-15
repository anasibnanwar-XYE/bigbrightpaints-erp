$ErrorActionPreference = "Stop"

Write-Host "Stopping CLIProxyAPI in WSL..."
wsl bash -lc 'pkill -f cli-proxy-api 2>/dev/null || true'
Write-Host "Done."

