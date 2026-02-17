# Task Packet

Ticket: `TKT-ERP-STAGE-066`
Slice: `SLICE-01`
Primary Agent: `orchestrator`
Reviewers: `qa-reliability, repo-cartographer, security-governance`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-066/orchestrator`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_worktrees/TKT-ERP-STAGE-066/orchestrator`

## Objective
Close final staging go/no-go with reviewer completeness and zero unresolved P0 blockers on integration SHA

## Agent Write Boundary (Enforced)
- `docs/`
- `agents/`
- `skills/`
- `ci/`
- `tickets/`
- `asyncloop`
- `scripts/harness_orchestrator.py`

## Requested Focus Paths
- `tickets/TKT-ERP-STAGE-061/ticket.yaml`
- `tickets/TKT-ERP-STAGE-062/ticket.yaml`
- `tickets/TKT-ERP-STAGE-065/ticket.yaml`

## Required Checks Before Done
- `bash ci/lint-knowledgebase.sh`
- `bash ci/check-architecture.sh`
- `bash ci/check-enterprise-policy.sh`

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
You are `orchestrator`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- identity line: `I am orchestrator and I own SLICE-01.`
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
