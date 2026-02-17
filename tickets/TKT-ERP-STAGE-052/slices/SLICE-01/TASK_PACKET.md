# Task Packet

Ticket: `TKT-ERP-STAGE-052`
Slice: `SLICE-01`
Primary Agent: `hr-domain`
Reviewers: `qa-reliability, security-governance`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-052/hr-domain`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_worktrees/TKT-ERP-STAGE-052/hr-domain`

## Objective
M18-S3 payroll posting canonicality: fail closed when POSTED payroll run lacks journal linkage to prevent duplicate postings

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/hr/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/payroll/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollService.java`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/hr/PayrollRunIdempotencyIT.java`

## Cross-Workflow Dependencies
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: auth-rbac-company
- External downstream agents to watch: accounting-domain
- Contract edges:
  - downstream-external -> accounting-domain: payroll liability/payment posting linkage
  - upstream-external -> auth-rbac-company: payroll/PII access boundaries

## Required Checks Before Done
- `cd erp-domain && mvn -B -ntp -Dtest='*Payroll*' test`
- `bash scripts/verify_local.sh`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `hr-domain`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
