# Ticket TKT-ERP-STAGE-098

- title: Gate Fast Threshold Closure Tranche 1
- goal: Raise anchored gate_fast changed-files line/branch coverage by adding deterministic tests for highest-deficit tenant runtime, company, admin/portal, purchasing, sales, and accounting-period services.
- priority: high
- status: in_progress
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-20T10:48:51+00:00
- updated_at: 2026-02-20T19:08:59+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | accounting-domain | w1 | merged | `tickets/tkt-erp-stage-098/accounting-domain` |
| SLICE-02 | auth-rbac-company | w2 | merged | `tickets/tkt-erp-stage-098/auth-rbac-company` |
| SLICE-03 | purchasing-invoice-p2p | w3 | ready | `tickets/tkt-erp-stage-098/purchasing-invoice-p2p` |
| SLICE-04 | reports-admin-portal | w4 | merged | `tickets/tkt-erp-stage-098/reports-admin-portal` |
| SLICE-05 | sales-domain | w1 | ready | `tickets/tkt-erp-stage-098/sales-domain` |
| SLICE-06 | refactor-techdebt-gc | w2 | ready | `tickets/tkt-erp-stage-098/refactor-techdebt-gc` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp/tickets/TKT-ERP-STAGE-098/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-098`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-098`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-098 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-098 --merge --cleanup-worktrees`
