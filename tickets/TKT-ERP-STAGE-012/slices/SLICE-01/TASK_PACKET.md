# Task Packet

Ticket: `TKT-ERP-STAGE-012`
Slice: `SLICE-01`
Primary Agent: `refactor-techdebt-gc`
Reviewers: `qa-reliability`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-012/refactor-techdebt-gc`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo_worktrees/TKT-ERP-STAGE-012/refactor-techdebt-gc`

## Objective
M18-S5 smallest closure: lock supplier settlement split-allocation and over-allocation guard contracts

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/`
- `erp-domain/src/test/java/`
- `docs/`

## Requested Focus Paths
- `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/p2p/TS_P2PPurchaseSettlementBoundaryTest.java`

## Required Checks Before Done
- `bash ci/check-architecture.sh`
- `cd erp-domain && mvn -B -ntp -Dtest=TS_P2PPurchaseSettlementBoundaryTest test`

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
You are `refactor-techdebt-gc`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
