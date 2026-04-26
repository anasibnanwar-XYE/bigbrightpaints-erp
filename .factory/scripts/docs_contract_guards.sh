#!/usr/bin/env bash
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
bash ci/check-enterprise-policy.sh
bash ci/check-architecture.sh
bash ci/check-orchestrator-layer.sh
python3 scripts/check_flaky_tags.py --tests-root erp-domain/src/test/java --gate gate-fast
bash scripts/guard_openapi_contract_drift.sh
