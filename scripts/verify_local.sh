#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[verify_local] root=$ROOT_DIR"

echo "[verify_local] schema drift scan"
bash "$ROOT_DIR/scripts/schema_drift_scan.sh"

echo "[verify_local] flyway overlap scan (heuristic)"
bash "$ROOT_DIR/scripts/flyway_overlap_scan.sh"

echo "[verify_local] time api scan"
bash "$ROOT_DIR/scripts/time_api_scan.sh"

if [[ "${VERIFY_LOCAL_SKIP_TESTS:-false}" == "true" ]]; then
  echo "[verify_local] mvn verify (skip tests)"
  (cd "$ROOT_DIR/erp-domain" && mvn -B -ntp -DskipTests -Djacoco.skip=true verify)
else
  echo "[verify_local] mvn verify"
  (cd "$ROOT_DIR/erp-domain" && mvn -B -ntp verify)
fi

echo "[verify_local] OK"
