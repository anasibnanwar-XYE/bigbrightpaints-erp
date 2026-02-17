# Task Packet

Ticket: `TKT-ERP-STAGE-060`
Slice: `SLICE-03`
Primary Agent: `frontend-documentation`
Reviewers: `repo-cartographer`
Lane: `w3`
Branch: `tickets/tkt-erp-stage-060/frontend-documentation`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_worktrees/TKT-ERP-STAGE-060/frontend-documentation`

## Objective
Align tenant quota API contract, OpenAPI artifacts, and portal documentation with runtime enforcement

## Agent Write Boundary (Enforced)
- `docs/accounting-portal-endpoint-map.md`
- `docs/admin-portal-endpoint-map.md`
- `docs/dealer-portal-endpoint-map.md`
- `docs/manufacturing-portal-endpoint-map.md`
- `docs/sales-portal-endpoint-map.md`
- `docs/*portal*handoff*.md`

## Requested Focus Paths
- `docs/admin-portal-endpoint-map.md`

## Required Checks Before Done
- `bash ci/lint-knowledgebase.sh`

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
You are `frontend-documentation`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- identity line: `I am frontend-documentation and I own SLICE-03.`
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
