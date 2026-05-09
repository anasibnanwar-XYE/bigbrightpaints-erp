#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${OPENAPI_CONTRACT_DRIFT_MODE:-verify}"
OPENAPI_SPEC="${OPENAPI_CONTRACT_DRIFT_OPENAPI_SPEC:-$ROOT_DIR/openapi.json}"
OPENAPI_ENDPOINT_CONTRACT_DOC="${OPENAPI_CONTRACT_DRIFT_OPENAPI_ENDPOINT_CONTRACT_DOC:-$ROOT_DIR/docs/openapi-endpoint-contract.md}"
REMEDIATION_COMMAND="${OPENAPI_CONTRACT_DRIFT_REMEDIATION_COMMAND:-OPENAPI_CONTRACT_DRIFT_MODE=report bash scripts/guard_openapi_contract_drift.sh}"

fail() {
  echo "[guard_openapi_contract_drift] FAIL: $1" >&2
  echo "[guard_openapi_contract_drift] REMEDIATION: run '$REMEDIATION_COMMAND'" >&2
  exit 1
}

[[ -f "$OPENAPI_SPEC" ]] || fail "missing required file: $OPENAPI_SPEC"
if [[ ! -f "$OPENAPI_ENDPOINT_CONTRACT_DOC" ]]; then
  fail "missing openapi endpoint contract doc: $OPENAPI_ENDPOINT_CONTRACT_DOC"
fi

case "$MODE" in
  verify|report)
    ;;
  *)
    fail "invalid OPENAPI_CONTRACT_DRIFT_MODE='$MODE' (expected verify or report)"
    ;;
esac

python3 - "$MODE" "$OPENAPI_SPEC" "$OPENAPI_ENDPOINT_CONTRACT_DOC" "$REMEDIATION_COMMAND" <<'PY'
import collections
import hashlib
import json
import re
import sys
from pathlib import Path

mode, openapi_path, contract_path, remediation_command = sys.argv[1:5]
http_methods = {"get", "post", "put", "patch", "delete", "options", "head", "trace"}

openapi_raw = Path(openapi_path).read_bytes()
openapi_sha256 = hashlib.sha256(openapi_raw).hexdigest()
spec = json.loads(openapi_raw.decode("utf-8"))

paths = spec.get("paths")
if not isinstance(paths, dict):
    print("[guard_openapi_contract_drift] FAIL: OpenAPI spec missing object 'paths'", file=sys.stderr)
    print(f"[guard_openapi_contract_drift] REMEDIATION: run '{remediation_command}'", file=sys.stderr)
    raise SystemExit(1)

openapi_entries = set()
superadmin_success_schema_errors = []
for path, path_item in paths.items():
    if not isinstance(path_item, dict):
        continue
    for method, operation in path_item.items():
        if method.lower() not in http_methods:
            continue
        openapi_entries.add((method.upper(), path))
        if not path.startswith("/api/v1/superadmin") or not isinstance(operation, dict):
            continue
        responses = operation.get("responses")
        if not isinstance(responses, dict):
            superadmin_success_schema_errors.append(
                f"{method.upper()} {path} has no responses object"
            )
            continue
        for response_code, response in responses.items():
            if not str(response_code).startswith("2"):
                continue
            content = response.get("content") if isinstance(response, dict) else None
            if not isinstance(content, dict) or not content:
                superadmin_success_schema_errors.append(
                    f"{method.upper()} {path} response {response_code} has no schema content"
                )
                continue
            schema = None
            for media_type in ("*/*", "application/json"):
                media = content.get(media_type)
                if isinstance(media, dict):
                    schema = media.get("schema")
                    if isinstance(schema, dict):
                        break
            schema_ref = schema.get("$ref") if isinstance(schema, dict) else None
            if not isinstance(schema_ref, str) or not schema_ref.startswith("#/components/schemas/ApiResponse"):
                superadmin_success_schema_errors.append(
                    f"{method.upper()} {path} response {response_code} uses non-ApiResponse schema"
                )

openapi_total_paths = len(paths)
openapi_total_operations = len(openapi_entries)

contract_text = Path(contract_path).read_text(encoding="utf-8")
contract_line_re = re.compile(r"^- `([A-Z]+(?:, [A-Z]+)*)` `(/api(?:/v1)?/[^`]+)`\r?$")

contract_entries = []
for raw_line in contract_text.splitlines():
    match = contract_line_re.match(raw_line)
    if not match:
        continue
    methods = [value.strip() for value in match.group(1).split(",")]
    endpoint_path = match.group(2)
    for method in methods:
        contract_entries.append((method, endpoint_path))

contract_counter = collections.Counter(contract_entries)
contract_duplicates = sorted((entry, count) for entry, count in contract_counter.items() if count > 1)
contract_entry_set = set(contract_entries)
contract_total_operations = len(contract_entry_set)
contract_total_paths = len({path for _, path in contract_entry_set})

declared_sha_match = re.search(
    r"OpenAPI snapshot:\s*`openapi\.json`\s*\(sha256\s*`([0-9a-f]{64})`\)",
    contract_text,
)
declared_paths_match = re.search(r"OpenAPI total paths:\s*`([0-9]+)`", contract_text)
declared_operations_match = re.search(r"OpenAPI total operations:\s*`([0-9]+)`", contract_text)

declared_sha = declared_sha_match.group(1) if declared_sha_match else None
declared_paths = int(declared_paths_match.group(1)) if declared_paths_match else None
declared_operations = int(declared_operations_match.group(1)) if declared_operations_match else None

missing_from_contract = sorted(openapi_entries - contract_entry_set)
extra_in_contract = sorted(contract_entry_set - openapi_entries)

if mode == "report":
    print("[guard_openapi_contract_drift] REPORT")
    print(f"[guard_openapi_contract_drift] openapi_sha256={openapi_sha256}")
    print(f"[guard_openapi_contract_drift] openapi_total_paths={openapi_total_paths}")
    print(f"[guard_openapi_contract_drift] openapi_total_operations={openapi_total_operations}")
    print(f"[guard_openapi_contract_drift] contract_total_paths={contract_total_paths}")
    print(f"[guard_openapi_contract_drift] contract_total_operations={contract_total_operations}")
    print(
        "[guard_openapi_contract_drift] expected_contract_snapshot_line="
        f"OpenAPI snapshot: `openapi.json` (sha256 `{openapi_sha256}`)"
    )
    print(
        "[guard_openapi_contract_drift] expected_contract_paths_line="
        f"OpenAPI total paths: `{openapi_total_paths}`"
    )
    print(
        "[guard_openapi_contract_drift] expected_contract_operations_line="
        f"OpenAPI total operations: `{openapi_total_operations}`"
    )
    if contract_duplicates:
        preview = ", ".join(f"{method} {path} (x{count})" for (method, path), count in contract_duplicates[:5])
        print(f"[guard_openapi_contract_drift] contract_duplicates={preview}")
    if missing_from_contract:
        preview = ", ".join(f"{method} {path}" for method, path in missing_from_contract[:5])
        print(f"[guard_openapi_contract_drift] missing_from_contract={preview}")
    if extra_in_contract:
        preview = ", ".join(f"{method} {path}" for method, path in extra_in_contract[:5])
        print(f"[guard_openapi_contract_drift] extra_in_contract={preview}")
    if superadmin_success_schema_errors:
        preview = "; ".join(superadmin_success_schema_errors[:5])
        print(f"[guard_openapi_contract_drift] superadmin_success_schema_errors={preview}")
    raise SystemExit(0)

errors = []

if declared_sha is None:
    errors.append("docs/openapi-endpoint-contract.md is missing the OpenAPI snapshot sha256 line")
elif declared_sha != openapi_sha256:
    errors.append(
        "OpenAPI snapshot sha256 mismatch "
        f"(declared={declared_sha}, actual={openapi_sha256})"
    )

if declared_paths is None:
    errors.append("docs/openapi-endpoint-contract.md is missing the OpenAPI total paths line")
elif declared_paths != openapi_total_paths:
    errors.append(
        "OpenAPI total paths mismatch "
        f"(declared={declared_paths}, actual={openapi_total_paths})"
    )

if declared_operations is None:
    errors.append("docs/openapi-endpoint-contract.md is missing the OpenAPI total operations line")
elif declared_operations != openapi_total_operations:
    errors.append(
        "OpenAPI total operations mismatch "
        f"(declared={declared_operations}, actual={openapi_total_operations})"
    )

if contract_duplicates:
    preview = ", ".join(f"{method} {path} (x{count})" for (method, path), count in contract_duplicates[:5])
    errors.append(f"duplicate method/path entries found in docs/openapi-endpoint-contract.md ({preview})")

if contract_total_operations != openapi_total_operations:
    errors.append(
        "docs/openapi-endpoint-contract.md method/path contract size mismatch "
        f"(contract={contract_total_operations}, openapi={openapi_total_operations})"
    )

if contract_total_paths != openapi_total_paths:
    errors.append(
        "docs/openapi-endpoint-contract.md unique path count mismatch "
        f"(contract={contract_total_paths}, openapi={openapi_total_paths})"
    )

if missing_from_contract:
    preview = ", ".join(f"{method} {path}" for method, path in missing_from_contract[:5])
    errors.append(f"OpenAPI endpoints missing from docs/openapi-endpoint-contract.md ({preview})")

if extra_in_contract:
    preview = ", ".join(f"{method} {path}" for method, path in extra_in_contract[:5])
    errors.append(f"docs/openapi-endpoint-contract.md contains endpoints absent from OpenAPI ({preview})")

if superadmin_success_schema_errors:
    preview = "; ".join(superadmin_success_schema_errors[:5])
    errors.append(
        "Super Admin 2xx responses must use non-empty ApiResponse schemas "
        f"({preview})"
    )

if errors:
    print(
        "[guard_openapi_contract_drift] FAIL: OpenAPI contract drift detected in verification mode",
        file=sys.stderr,
    )
    for issue in errors:
        print(f"[guard_openapi_contract_drift] FAIL: {issue}", file=sys.stderr)
    print(f"[guard_openapi_contract_drift] REMEDIATION: run '{remediation_command}'", file=sys.stderr)
    raise SystemExit(1)

print("[guard_openapi_contract_drift] OK")
PY
