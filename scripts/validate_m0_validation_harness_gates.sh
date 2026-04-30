#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT="$(cd -- "${SCRIPT_DIR}/.." >/dev/null 2>&1 && pwd)"
MODE="${1:---static}"

fail() {
  echo "[m0-validation-harness] FAIL: $*" >&2
  exit 1
}

note() {
  echo "[m0-validation-harness] $*"
}

require_file() {
  [[ -f "$1" ]] || fail "required file missing: $1"
}

require_contains() {
  local file="$1"
  local needle="$2"
  grep -Fq -- "$needle" "$file" || fail "$file does not contain required text: $needle"
}

assert_secret_ignore_gate() {
  cd "$ROOT"
  local ignored
  for ignored in .env .env.local .env.production .env.validation.local; do
    git check-ignore -q -- "$ignored" || fail "$ignored must be ignored"
  done
  if [[ -e .env.example ]] && git check-ignore -q -- .env.example; then
    fail ".env.example must remain trackable"
  fi
  local tracked_secret_files
  tracked_secret_files="$(git ls-files -- .env '.env.*' | grep -Ev '^\.env\.example$' || true)"
  [[ -z "$tracked_secret_files" ]] || fail "tracked local secret files detected"
  note "secret_ignore_gate=passed files=.env,.env.* trackable=.env.example"
}

assert_profile_gates() {
  local harness_dir="$ROOT/erp-domain/src/main/java/com/bigbrightpaints/erp/core/validationharness"
  local clock="$ROOT/erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/CompanyClock.java"
  local security="$ROOT/erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/SecurityConfig.java"
  local seed="$ROOT/erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/ValidationSeedDataInitializer.java"
  local health="$ROOT/erp-domain/src/main/java/com/bigbrightpaints/erp/core/health/RequiredConfigHealthIndicator.java"

  require_file "$harness_dir/ValidationHarnessController.java"
  require_file "$harness_dir/ValidationFaultInjectionService.java"
  require_file "$harness_dir/ValidationTimeControlService.java"
  require_file "$harness_dir/ValidationFaultHealthIndicator.java"
  require_file "$harness_dir/ValidationSecurityAlertTriggerService.java"
  require_contains "$harness_dir/ValidationHarnessController.java" '@Profile("validation-harness")'
  require_contains "$harness_dir/ValidationFaultInjectionService.java" '@Profile("validation-harness")'
  require_contains "$harness_dir/ValidationTimeControlService.java" '@Profile("validation-harness")'
  require_contains "$harness_dir/ValidationFaultHealthIndicator.java" '@Profile("validation-harness")'
  require_contains "$harness_dir/ValidationSecurityAlertTriggerService.java" '@Profile("validation-harness")'
  require_contains "$security" 'Profiles.of("validation-harness")'
  require_contains "$security" '"/api/v1/validation/harness/**"'
  require_contains "$clock" 'ValidationTimeControlService'
  require_contains "$harness_dir/ValidationRunNamespace.java" 'SAFE_RUN_MARKER'
  require_contains "$seed" '@Profile("validation-seed")'
  require_contains "$seed" 'ensureMockProfileEnabled(environment)'
  require_contains "$health" 'auditSigningConfigured'
  require_contains "$health" 'datasourceConfigured'

  note "profile_gates=passed validation-harness-and-validation-seed-helpers-gated"
}

assert_secret_scan() {
  cd "$ROOT"
  local targets=()
  [[ -f "$ROOT/openapi.json" ]] && targets+=("$ROOT/openapi.json")
  [[ -d "$ROOT/artifacts" ]] && targets+=("$ROOT/artifacts")
  [[ -d "$ROOT/.factory/validation" ]] && targets+=("$ROOT/.factory/validation")
  [[ -d "$ROOT/testing" ]] && targets+=("$ROOT/testing")

  python3 - "${targets[@]}" <<'PY'
import os
import re
import sys
from pathlib import Path

PATTERNS = [
    ("private-key-block", re.compile(r"-----BEGIN (?:RSA |OPENSSH |EC |DSA )?PRIVATE KEY-----")),
    ("jwt-like-token", re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}")),
    ("aws-access-key", re.compile(r"AKIA[0-9A-Z]{16}")),
    ("slack-token", re.compile(r"xox[baprs]-[A-Za-z0-9-]{20,}")),
    (
        "activation-or-reset-url-token",
        re.compile(
            r"https?://[^\s\"']*/(?:activate|activation|reset-password|password-reset)[^\s\"']*(?:token=|/)[A-Za-z0-9_-]{20,}",
            re.IGNORECASE,
        ),
    ),
    (
        "live-secret-assignment",
        re.compile(
            r"\b(?:SENTRY_AUTH_TOKEN|DD_API_KEY|JWT_SECRET|ERP_SECURITY_ENCRYPTION_KEY|ERP_SECURITY_AUDIT_PRIVATE_KEY|SPRING_DATASOURCE_PASSWORD|SPRING_MAIL_PASSWORD)\s*[:=]\s*['\"]?[A-Za-z0-9_./+=-]{12,}",
        ),
    ),
]

SELF_TEST = (
    "token="
    + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    + ".aaaaaaaaaaaaaaaa"
    + ".bbbbbbbbbbbbbbbb"
)
if not any(pattern.search(SELF_TEST) for _, pattern in PATTERNS):
    raise SystemExit("secret scanner self-test failed")

def iter_files(paths):
    for raw in paths:
        path = Path(raw)
        if path.is_file():
            yield path
            continue
        if path.is_dir():
            for root, dirs, files in os.walk(path):
                dirs[:] = [d for d in dirs if d not in {".git", "target", "node_modules"}]
                for name in files:
                    candidate = Path(root, name)
                    if candidate.stat().st_size <= 2_000_000:
                        yield candidate

findings = []
for file_path in iter_files(sys.argv[1:]):
    try:
        text = file_path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        continue
    for line_no, line in enumerate(text.splitlines(), start=1):
        for label, pattern in PATTERNS:
            if pattern.search(line):
                findings.append(f"{file_path}:{line_no}:{label}")

if findings:
    print("secret_scan_findings=" + ",".join(findings))
    raise SystemExit(1)
print(f"secret_scan=passed scanned_files={sum(1 for _ in iter_files(sys.argv[1:]))}")
PY
}

case "$MODE" in
  --static)
    assert_secret_ignore_gate
    assert_profile_gates
    assert_secret_scan
    note "namespace_isolation=passed safe-run-marker-required"
    ;;
  *)
    fail "unknown mode: $MODE"
    ;;
esac
