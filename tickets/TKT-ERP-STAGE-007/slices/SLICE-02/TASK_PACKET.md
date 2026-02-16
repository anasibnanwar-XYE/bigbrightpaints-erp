# Task Packet

Ticket: `TKT-ERP-STAGE-007`
Slice: `SLICE-02`
Primary Agent: `purchasing-invoice-p2p`
Reviewers: `qa-reliability, security-governance`
Lane: `w2`
Branch: `tickets/tkt-erp-stage-007/purchasing-invoice-p2p`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo_worktrees/TKT-ERP-STAGE-007/purchasing-invoice-p2p`

## Objective
M18-S6A smallest shippable closure: enforce GST/non-GST settlement posting drift guards and reconciliation-safe contracts

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/purchasing/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/invoice/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/p2p/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/p2p/`

## Cross-Workflow Dependencies
- Upstream slices: none
- Downstream slices: SLICE-01
- External upstream agents to watch: auth-rbac-company
- External downstream agents to watch: inventory-domain
- Contract edges:
  - downstream -> accounting-domain (slice SLICE-01): ap/posting and settlement linkage
  - downstream-external -> inventory-domain: grn/stock intake coupling
  - upstream-external -> auth-rbac-company: tenant-scoped supplier/AP access rules

## Required Checks Before Done
- `cd erp-domain && mvn -B -ntp -Dtest='TS_P2PPurchaseJournalLinkageTest,GstConfigurationRegressionIT' test`
- `bash ci/check-enterprise-policy.sh`
- `bash ci/check-architecture.sh`

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
You are `purchasing-invoice-p2p`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
