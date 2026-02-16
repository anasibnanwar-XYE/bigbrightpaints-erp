# Ticket TKT-ERP-STAGE-001

- title: ERP Staging Batch 1
- goal: First shippable implementation from ERP_STAGING_MASTER_PLAN
- priority: high
- status: in_progress
- base_branch: async-loop-predeploy-audit
- created_at: 2026-02-16T08:32:02+00:00
- updated_at: 2026-02-16T08:44:26+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | w1 | waiting_for_push | `tickets/tkt-erp-stage-001/auth-rbac-company` |
| SLICE-02 | sales-domain | w2 | waiting_for_push | `tickets/tkt-erp-stage-001/sales-domain` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp/tickets/TKT-ERP-STAGE-001/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-001`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-001`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-001 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-001 --merge --cleanup-worktrees`
