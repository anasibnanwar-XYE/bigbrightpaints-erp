#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REMEDIATION_COMMAND="bash scripts/guard_workflow_canonical_paths.sh"
ERP_MAIN_DIR="$ROOT_DIR/erp-domain/src/main/java/com/bigbrightpaints/erp"
SALES_CORE_ENGINE="$ERP_MAIN_DIR/modules/sales/service/SalesCoreEngine.java"
COMMAND_DISPATCHER="$ERP_MAIN_DIR/orchestrator/service/CommandDispatcher.java"
INTEGRATION_COORDINATOR="$ERP_MAIN_DIR/orchestrator/service/IntegrationCoordinator.java"
ORCHESTRATOR_DIR="$ERP_MAIN_DIR/orchestrator"

fail() {
  echo "[guard_workflow_canonical_paths] FAIL: $1" >&2
  echo "[guard_workflow_canonical_paths] remediation: run '$REMEDIATION_COMMAND'" >&2
  exit 1
}

for path in \
  "$SALES_CORE_ENGINE" \
  "$COMMAND_DISPATCHER" \
  "$INTEGRATION_COORDINATOR"; do
  [[ -f "$path" ]] || fail "missing required source file: $path"
done

python3 - "$SALES_CORE_ENGINE" "$COMMAND_DISPATCHER" "$INTEGRATION_COORDINATOR" "$ORCHESTRATOR_DIR" <<'PY' || fail "canonical dispatch source checks failed"
from pathlib import Path
import re
import sys

sales_core_path = Path(sys.argv[1])
command_dispatcher_path = Path(sys.argv[2])
integration_coordinator_path = Path(sys.argv[3])
orchestrator_dir = Path(sys.argv[4])

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")

def fail(message: str) -> None:
    print(f"[guard_workflow_canonical_paths] FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)

def extract_method_body(text: str, signature_pattern: str, label: str) -> str:
    match = re.search(signature_pattern, text, re.MULTILINE)
    if not match:
        fail(f"missing {label}")
    brace_start = text.find("{", match.end() - 1)
    if brace_start == -1:
        fail(f"missing opening brace for {label}")
    depth = 0
    for index in range(brace_start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[brace_start + 1:index]
    fail(f"unterminated method body for {label}")

sales_core = read(sales_core_path)
dispatch_body = extract_method_body(
    sales_core,
    r"public\s+DispatchConfirmResponse\s+confirmDispatch\s*\([^)]*\)\s*\{",
    "SalesCoreEngine.confirmDispatch"
)
for required_call, label in [
    ("accountingFacade.postCogsJournal(", "AccountingFacade.postCogsJournal call inside SalesCoreEngine.confirmDispatch"),
    ("accountingFacade.postSalesJournal(", "AccountingFacade.postSalesJournal call inside SalesCoreEngine.confirmDispatch"),
]:
    if required_call not in dispatch_body:
        fail(f"missing {label}")

command_dispatcher = read(command_dispatcher_path)
dispatch_batch_body = extract_method_body(
    command_dispatcher,
    r"public\s+String\s+dispatchBatch\s*\([^)]*\)\s*\{",
    "CommandDispatcher.dispatchBatch"
)
if "throw new OrchestratorFeatureDisabledException(" not in dispatch_batch_body:
    fail("CommandDispatcher.dispatchBatch must fail closed instead of dispatching through an alternate posting path")
if "/api/v1/sales/dispatch/confirm" not in dispatch_batch_body and "SALES_DISPATCH_CANONICAL_PATH" not in dispatch_batch_body:
    fail("CommandDispatcher.dispatchBatch must point callers to /api/v1/sales/dispatch/confirm")

integration_coordinator = read(integration_coordinator_path)
update_fulfillment_body = extract_method_body(
    integration_coordinator,
    r"public\s+AutoApprovalResult\s+updateFulfillment\s*\(\s*String\s+orderId,\s*String\s+requestedStatus,\s*String\s+companyId,\s*String\s+traceId,\s*String\s+idempotencyKey\s*\)\s*\{",
    "IntegrationCoordinator.updateFulfillment"
)
for token in ["SHIPPED", "DISPATCHED", "FULFILLED", "COMPLETED"]:
    if f'case "{token}":' not in update_fulfillment_body:
        fail(f"IntegrationCoordinator.updateFulfillment must explicitly reject dispatch-like status {token}")
if "/api/v1/sales/dispatch/confirm" not in update_fulfillment_body:
    fail("IntegrationCoordinator.updateFulfillment must direct callers to /api/v1/sales/dispatch/confirm")

forbidden_patterns = {
    "postDispatchJournal(": "removed orchestrator dispatch journal method",
    "createAccountingEntry(": "removed orchestrator accounting-entry helper",
    "DISPATCH-": "legacy orchestrator DISPATCH-* journal namespace",
    ".postSalesJournal(": "orchestrator-side sales journal posting",
    ".postCogsJournal(": "orchestrator-side COGS journal posting",
}

violations: list[str] = []
for path in sorted(orchestrator_dir.rglob("*.java")):
    text = read(path)
    relative = path.relative_to(orchestrator_dir)
    for needle, label in forbidden_patterns.items():
        if needle in text:
            violations.append(f"{relative}: found {label} via '{needle}'")

if violations:
    fail(" ; ".join(violations))

print("[guard_workflow_canonical_paths] verified canonical dispatch source path")
PY

echo "[guard_workflow_canonical_paths] OK"
