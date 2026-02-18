# Ticket TKT-ERP-STAGE-068

- title: Purchase Return Idempotency Root-Cause Fix
- goal: Fix deterministic PurchaseReturnIdempotencyRegressionIT failure blocking strict-lane merges
- priority: high
- status: done
- base_branch: tmp/orch-exec-20260217
- created_at: 2026-02-18T06:02:05+00:00
- updated_at: 2026-02-18T06:23:11+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | purchasing-invoice-p2p | w1 | merged | `tickets/tkt-erp-stage-068/purchasing-invoice-p2p` |
| SLICE-02 | purchasing-invoice-p2p | w2 | merged | `tickets/tkt-erp-stage-068/refactor-techdebt-gc` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec/tickets/TKT-ERP-STAGE-068/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-068`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-068`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-068 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-068 --merge --cleanup-worktrees`
