#!/usr/bin/env bash
set -euo pipefail
BASE_URL=${BASE_URL:-http://localhost:8081}
COMPANY_CODE=${COMPANY_CODE:-BBP}
TOKEN=${TOKEN:?TOKEN required}
EMPLOYEE_ID=${EMPLOYEE_ID:?EMPLOYEE_ID required}
REQ=${REQ:?REQ required}

curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Company-Id: ${COMPANY_CODE}" \
  -H 'Content-Type: application/json' \
  --data @"${REQ}" \
  "${BASE_URL}/api/v1/hr/attendance/mark/${EMPLOYEE_ID}"
