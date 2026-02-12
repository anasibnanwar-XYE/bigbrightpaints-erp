#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DISPATCHER="$ROOT_DIR/erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/CommandDispatcher.java"
COORDINATOR="$ROOT_DIR/erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java"
COORDINATOR_TEST="$ROOT_DIR/erp-domain/src/test/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinatorTest.java"
REMEDIATION_COMMAND="bash scripts/guard_orchestrator_correlation_contract.sh"

fail() {
  echo "[guard_orchestrator_correlation_contract] ERROR: $1" >&2
  echo "[guard_orchestrator_correlation_contract] REMEDIATION: run '$REMEDIATION_COMMAND'" >&2
  exit 1
}

for path in "$DISPATCHER" "$COORDINATOR" "$COORDINATOR_TEST"; do
  [[ -f "$path" ]] || fail "missing required file: $path"
done

# Dispatcher must propagate trace/idempotency into both accounting side-effect calls.
rg -U -q "postDispatchJournal\\([\\s\\S]*request\\.postingAmount\\(\\),[\\s\\S]*traceId,[\\s\\S]*idempotencyKey\\)" "$DISPATCHER" \
  || fail "CommandDispatcher.dispatchBatch does not pass traceId/idempotencyKey to postDispatchJournal"
rg -U -q "recordPayrollPayment\\([\\s\\S]*request\\.creditAccountId\\(\\),[\\s\\S]*companyId,[\\s\\S]*traceId,[\\s\\S]*idempotencyKey\\)" "$DISPATCHER" \
  || fail "CommandDispatcher.runPayroll does not pass traceId/idempotencyKey to recordPayrollPayment"

# Coordinator must expose overloads that accept correlation fields.
rg -U -q "public void postDispatchJournal\\(String batchId,[\\s\\S]*String traceId,[\\s\\S]*String idempotencyKey\\)" "$COORDINATOR" \
  || fail "IntegrationCoordinator missing correlation-aware postDispatchJournal overload"
rg -U -q "public JournalEntryDto recordPayrollPayment\\(Long payrollRunId,[\\s\\S]*String traceId,[\\s\\S]*String idempotencyKey\\)" "$COORDINATOR" \
  || fail "IntegrationCoordinator missing correlation-aware recordPayrollPayment overload"

# Regression tests must pin correlation memo propagation.
rg -q "postDispatchJournalPropagatesTraceAndIdempotencyInMemo" "$COORDINATOR_TEST" \
  || fail "IntegrationCoordinatorTest missing dispatch correlation memo assertion"
rg -q "recordPayrollPaymentPropagatesTraceAndIdempotencyInMemo" "$COORDINATOR_TEST" \
  || fail "IntegrationCoordinatorTest missing payroll correlation memo assertion"

echo "[guard_orchestrator_correlation_contract] OK"
