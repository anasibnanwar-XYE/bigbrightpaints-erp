# Task Packet

Ticket: `TKT-ERP-STAGE-060`
Slice: `SLICE-05`
Primary Agent: `repo-cartographer`
Reviewers: `orchestrator`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-060/repo-cartographer`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_worktrees/TKT-ERP-STAGE-060/repo-cartographer`

## Objective
Align tenant quota API contract, OpenAPI artifacts, and portal documentation with runtime enforcement

## Agent Write Boundary (Enforced)
- `docs/`
- `ARCHITECTURE.md`
- `AGENTS.md`

## Requested Focus Paths
- `docs/system-map/Goal/ERP_STAGING_MASTER_PLAN.md`

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
You are `repo-cartographer`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- identity line: `I am repo-cartographer and I own SLICE-05.`
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
