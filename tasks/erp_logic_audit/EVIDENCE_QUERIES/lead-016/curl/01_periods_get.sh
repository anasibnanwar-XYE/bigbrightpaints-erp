#!/usr/bin/env bash
set -euo pipefail

: "${BASE_URL:?BASE_URL required}"
: "${TOKEN:?TOKEN required}"
: "${COMPANY_CODE:?COMPANY_CODE required}"

curl -sS -w "\nHTTP_STATUS:%{http_code}\n" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Company-Id: ${COMPANY_CODE}" \
  "${BASE_URL}/api/v1/accounting/periods"
