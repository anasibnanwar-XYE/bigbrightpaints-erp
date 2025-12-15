param(
  [string]$Distro = ""
)

$ErrorActionPreference = "Stop"

function Invoke-WSL([string]$Command) {
  $args = @()
  if ($Distro) { $args += @("-d", $Distro) }
  $args += @("--", "bash", "-lc", $Command)
  $output = & wsl.exe @args
  $code = $LASTEXITCODE
  if ($code -ne 0) {
    Write-Host "(wsl exit=$code)"
  }
  return $output
}

Write-Host "WSL process:"
Invoke-WSL 'pgrep -af "[c]li-proxy-api" || true'

Write-Host "`nWSL listening ports (8317):"
Invoke-WSL 'command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | grep ":8317" || true'

Write-Host "`nWSL log tail:"
Invoke-WSL 'ls -la ~/CLIProxyAPI/proxy.log 2>/dev/null || true; tail -n 50 ~/CLIProxyAPI/proxy.log 2>/dev/null || true'

Write-Host "`nWSL local HTTP test:"
Invoke-WSL 'code="$(curl -sS -o /dev/null -w "%{http_code}" http://127.0.0.1:8317/ || true)"; echo "$code"'

Write-Host "`nWindows localhost HTTP test:"
try {
  if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    $code = (curl.exe -sS -o NUL -w "%{http_code}" http://localhost:8317/).Trim()
    Write-Host $code
  } else {
    (Invoke-WebRequest -Uri "http://localhost:8317/" -Method GET -TimeoutSec 3).StatusCode
  }
} catch {
  Write-Host "0"
}

Write-Host "`nWSL IP (use if localhost forwarding is off):"
Invoke-WSL 'hostname -I | xargs -n1 | head -n 1'
