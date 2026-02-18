# Task Packet

Ticket: `TKT-ERP-STAGE-069`
Slice: `SLICE-02`
Primary Agent: `repo-cartographer`
Reviewers: `orchestrator`
Lane: `w2`
Branch: `tickets/tkt-erp-stage-069/repo-cartographer`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_worktrees/TKT-ERP-STAGE-069/repo-cartographer`

## Objective
Prevent duplicate verify runs and provide deterministic live progress during long checks

## Agent Write Boundary (Enforced)
- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/`
- `docs/ASYNC_LOOP_OPERATIONS.md`

## Requested Focus Paths
- `docs/ASYNC_LOOP_OPERATIONS.md`

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
- identity line: `I am repo-cartographer and I own SLICE-02.`
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
