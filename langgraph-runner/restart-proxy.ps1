param(
  [string]$Distro = ""
)

$ErrorActionPreference = "Stop"

function Invoke-WSL([string]$Command, [switch]$AllowFailure) {
  $args = @()
  if ($Distro) { $args += @("-d", $Distro) }
  $args += @("--", "bash", "-lc", $Command)
  $output = & wsl.exe @args
  $code = $LASTEXITCODE
  if (-not $AllowFailure -and $code -ne 0) {
    throw "WSL command failed (exit=$code): $Command"
  }
  return $output
}

Write-Host "Restarting CLIProxyAPI in WSL..."

Invoke-WSL 'set -euo pipefail; pkill -f "[c]li-proxy-api" 2>/dev/null || true' -AllowFailure
Invoke-WSL 'set -euo pipefail; cd ~/CLIProxyAPI; pwd; ls -la; test -x ./cli-proxy-api; test -f config.yaml; nohup ./cli-proxy-api --config config.yaml > proxy.log 2>&1 &'

Start-Sleep -Milliseconds 600

Write-Host "`nWSL proxy log tail:"
Invoke-WSL 'ls -la ~/CLIProxyAPI/proxy.log 2>/dev/null || true; tail -n 50 ~/CLIProxyAPI/proxy.log 2>/dev/null || true' -AllowFailure

Write-Host "`nWSL local check (should return 404 or 200):"
Invoke-WSL 'code="$(curl -sS -o /dev/null -w "%{http_code}" http://127.0.0.1:8317/ || true)"; echo "$code"' -AllowFailure
Invoke-WSL 'pgrep -af "[c]li-proxy-api" || true' -AllowFailure

Write-Host "`nWindows -> WSL localhost test:"
$body = '{"model":"gpt-5.2(xhigh)","input":"Reply with exactly: OK"}'
try {
  $code = $null
  if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    $code = (curl.exe -sS -o NUL -w "%{http_code}" http://localhost:8317/).Trim()
    if ($LASTEXITCODE -ne 0) { Write-Host "(curl exit=$LASTEXITCODE)" }
  } else {
    try {
      Invoke-WebRequest -Uri "http://localhost:8317/" -Method GET -TimeoutSec 3 | Out-Null
      $code = "200"
    } catch {
      if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
        $code = [int]$_.Exception.Response.StatusCode
      }
    }
  }
  if (-not $code) { $code = "0" }

  if ($code -eq "0" -or $code -eq "000") {
    throw "No response on http://localhost:8317/ (connection failed)"
  }

  $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
  if ($curl) {
    $resp = curl.exe -sS http://localhost:8317/v1/responses -H "Content-Type: application/json" -H "Authorization: Bearer dummy" -d $body
    if ($LASTEXITCODE -ne 0) { throw "curl.exe failed (exit=$LASTEXITCODE)" }
    $json = $resp | ConvertFrom-Json
  } else {
    $json = Invoke-RestMethod -Uri "http://localhost:8317/v1/responses" -Method POST -ContentType "application/json" -Headers @{ Authorization = "Bearer dummy" } -Body $body
  }
  if (-not $json -or -not $json.model) {
    throw "Proxy responded but did not return JSON with a model field"
  }
  Write-Host ("OK (model={0})" -f $json.model)
} catch {
  Write-Host "`nProxy test failed."
  Write-Host "Check inside WSL:"
  Write-Host "  wsl bash -lc 'tail -n 200 ~/CLIProxyAPI/proxy.log'"
  Write-Host "  wsl bash -lc 'curl -v http://127.0.0.1:8317/'"
  Write-Host ""
  Write-Host "If WSL works but Windows localhost does not, use the WSL IP:"
  Write-Host "  wsl bash -lc 'hostname -I | sed \"s/ .*//\"'"
  Write-Host "Then set:"
  Write-Host "  OPENAI_BASE_URL=http://<WSL_IP>:8317/v1"
  Write-Host "  Factory base_url=http://<WSL_IP>:8317/v1"
  throw
}

Write-Host "`nDone. Proxy should be on http://localhost:8317 (Factory base_url: http://localhost:8317/v1)"
