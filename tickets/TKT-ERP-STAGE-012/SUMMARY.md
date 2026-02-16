# Ticket TKT-ERP-STAGE-012

- title: ERP Staging Batch 12 - Settlement Idempotency Guard Hardening
- goal: M18-S5 smallest closure: lock supplier settlement split-allocation and over-allocation guard contracts
- priority: high
- status: done
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-16T21:37:58+00:00
- updated_at: 2026-02-16T21:39:10+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | refactor-techdebt-gc | w1 | merged | `tickets/tkt-erp-stage-012/refactor-techdebt-gc` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo/tickets/TKT-ERP-STAGE-012/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-012`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-012`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-012 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-012 --merge --cleanup-worktrees`
