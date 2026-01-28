#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="$ROOT_DIR/erp-domain/src/main/java/com/bigbrightpaints/erp"

PATTERN='LocalDate\.now\(|Instant\.now\(|ZoneId\.systemDefault\(|new Date\(|Clock\.systemDefaultZone\('

echo "[time_api_scan] scanning ${TARGET_DIR}"

if command -v rg >/dev/null 2>&1; then
  matches=$(rg -n "$PATTERN" "$TARGET_DIR" \
    --glob '!**/modules/auth/**' \
    --glob '!**/core/security/**' \
    --glob '!**/core/util/CompanyClock.java' \
    --glob '!**/core/util/CompanyTime.java' \
    || true)
else
  matches=$(grep -RInE "$PATTERN" "$TARGET_DIR" \
    --exclude-dir=auth \
    --exclude-dir=security \
    --exclude=CompanyClock.java \
    --exclude=CompanyTime.java \
    || true)
fi

if [[ -n "$matches" ]]; then
  echo "[time_api_scan] forbidden time API usage detected:" >&2
  echo "$matches" >&2
  exit 1
fi

echo "[time_api_scan] OK"
