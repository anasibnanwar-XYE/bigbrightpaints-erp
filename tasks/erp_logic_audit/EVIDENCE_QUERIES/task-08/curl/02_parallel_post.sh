#!/usr/bin/env bash
set -euo pipefail

: "${BASE_URL:?Set BASE_URL (e.g., http://localhost:8081)}"
: "${TOKEN:?Set TOKEN (JWT bearer)}"
: "${COMPANY_CODE:?Set COMPANY_CODE (for X-Company-Id)}"
: "${ENDPOINT:?Set ENDPOINT (e.g., /api/v1/sales/orders)}"
: "${REQUEST_FILE:?Set REQUEST_FILE (JSON body)}"
: "${OUTPUT_DIR:?Set OUTPUT_DIR (folder for responses)}"
: "${TAG:?Set TAG (response filename prefix)}"

PARALLELISM=${PARALLELISM:-4}

H_AUTH="Authorization: Bearer ${TOKEN}"
H_COMP="X-Company-Id: ${COMPANY_CODE}"

mkdir -p "${OUTPUT_DIR}"

for i in $(seq 1 "${PARALLELISM}"); do
  curl -sS -w "\nHTTP_STATUS:%{http_code}\n" \
    -H "${H_AUTH}" \
    -H "${H_COMP}" \
    -H 'Content-Type: application/json' \
    --data @"${REQUEST_FILE}" \
    "${BASE_URL}${ENDPOINT}" \
    > "${OUTPUT_DIR}/${TAG}_${i}.json" &
done

wait
