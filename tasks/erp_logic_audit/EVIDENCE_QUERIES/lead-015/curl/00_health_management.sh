#!/usr/bin/env bash
set -euo pipefail

MANAGEMENT_URL=${MANAGEMENT_URL:-http://localhost:19090}

curl -sS -w "\nHTTP_STATUS:%{http_code}\n" "${MANAGEMENT_URL}/actuator/health"
