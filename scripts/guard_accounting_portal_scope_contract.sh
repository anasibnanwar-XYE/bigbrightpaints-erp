#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GUARDRAIL_DOC="$ROOT_DIR/docs/ACCOUNTING_PORTAL_SCOPE_GUARDRAIL.md"
ENDPOINT_MAP_DOC="$ROOT_DIR/docs/accounting-portal-endpoint-map.md"
HANDOFF_DOC="$ROOT_DIR/docs/accounting-portal-frontend-engineer-handoff.md"
ENDPOINT_INVENTORY_DOC="$ROOT_DIR/docs/endpoint-inventory.md"
REMEDIATION_COMMAND="bash scripts/guard_accounting_portal_scope_contract.sh"
SCOPE_SENTENCE="HR, PURCHASING, INVENTORY, and REPORTS come under the Accounting portal"

fail() {
  echo "[guard_accounting_portal_scope_contract] ERROR: $1" >&2
  echo "[guard_accounting_portal_scope_contract] REMEDIATION: run '$REMEDIATION_COMMAND'" >&2
  exit 1
}

for path in "$GUARDRAIL_DOC" "$ENDPOINT_MAP_DOC" "$HANDOFF_DOC" "$ENDPOINT_INVENTORY_DOC"; do
  [[ -f "$path" ]] || fail "missing required file: $path"
done

for path in "$GUARDRAIL_DOC" "$ENDPOINT_MAP_DOC" "$HANDOFF_DOC" "$ENDPOINT_INVENTORY_DOC"; do
  rg -q "$SCOPE_SENTENCE" "$path" || fail "missing accounting portal scope invariant in $path"
done

rg -q "docs/ACCOUNTING_PORTAL_SCOPE_GUARDRAIL.md" "$ENDPOINT_MAP_DOC" \
  || fail "accounting endpoint map must reference the scope guardrail doc"
rg -q "docs/ACCOUNTING_PORTAL_SCOPE_GUARDRAIL.md" "$ENDPOINT_INVENTORY_DOC" \
  || fail "endpoint inventory must reference the scope guardrail doc"

rg -q "Change-Control Rule" "$GUARDRAIL_DOC" \
  || fail "scope guardrail doc must keep change-control section"
rg -q "Updated portal endpoint map and frontend handoff docs" "$GUARDRAIL_DOC" \
  || fail "scope guardrail doc must require portal-map + handoff updates for scope changes"
rg -q 'Updated `?docs/endpoint-inventory\.md`? module mapping and examples' "$GUARDRAIL_DOC" \
  || fail "scope guardrail doc must require endpoint inventory updates for scope changes"

echo "[guard_accounting_portal_scope_contract] OK"
