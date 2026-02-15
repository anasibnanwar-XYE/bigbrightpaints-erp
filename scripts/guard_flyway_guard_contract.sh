#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_RELEASE="$ROOT_DIR/scripts/gate_release.sh"
VERIFY_LOCAL="$ROOT_DIR/scripts/verify_local.sh"

require_text() {
  local file="$1"
  local needle="$2"
  local label="$3"
  if ! grep -Fq "$needle" "$file"; then
    echo "[guard_flyway_guard_contract] FAIL: missing contract marker for $label" >&2
    echo "[guard_flyway_guard_contract] expected in $file: $needle" >&2
    exit 1
  fi
}

for file in "$GATE_RELEASE" "$VERIFY_LOCAL"; do
  if [[ ! -f "$file" ]]; then
    echo "[guard_flyway_guard_contract] FAIL: missing script: $file" >&2
    exit 1
  fi
done

# gate_release invariants: explicit mismatch override flag + delegated propagation.
require_text "$GATE_RELEASE" 'ALLOW_GUARD_DB_MISMATCH="${ALLOW_FLYWAY_GUARD_DB_MISMATCH:-false}"' "gate_release mismatch override variable"
require_text "$GATE_RELEASE" 'if [[ "$ALLOW_GUARD_DB_MISMATCH" != "true" ]]; then' "gate_release fail-closed mismatch branch"
require_text "$GATE_RELEASE" 'ALLOW_FLYWAY_GUARD_DB_MISMATCH=true only when intentionally targeting different databases.' "gate_release mismatch override guidance"
require_text "$GATE_RELEASE" 'ALLOW_FLYWAY_GUARD_DB_MISMATCH="$ALLOW_GUARD_DB_MISMATCH"' "gate_release verify_local override propagation"
require_text "$GATE_RELEASE" 'VERIFY_LOCAL_SKIP_FLYWAY_GUARD="$VERIFY_LOCAL_SKIP_GUARD"' "gate_release guard delegation marker"

# verify_local invariants: fail-closed mismatch + required-mode/delegation bypass protections.
require_text "$VERIFY_LOCAL" 'ALLOW_GUARD_DB_MISMATCH="${ALLOW_FLYWAY_GUARD_DB_MISMATCH:-false}"' "verify_local mismatch override variable"
require_text "$VERIFY_LOCAL" 'if [[ "$ALLOW_GUARD_DB_MISMATCH" != "true" ]]; then' "verify_local fail-closed mismatch branch"
require_text "$VERIFY_LOCAL" 'if [[ "$SKIP_FLYWAY_GUARD" == "true" && "${REQUIRE_FLYWAY_V2_GUARD:-false}" == "true" ]]; then' "verify_local required-mode skip prevention"
require_text "$VERIFY_LOCAL" 'elif [[ "$SKIP_FLYWAY_GUARD" == "true" && "$DELEGATED_GUARD_EXECUTED" != "true" ]]; then' "verify_local delegation marker enforcement"
require_text "$VERIFY_LOCAL" 'SKIP_FLYWAY_GUARD=false' "verify_local forced guard execution reset"

echo "[guard_flyway_guard_contract] OK"
