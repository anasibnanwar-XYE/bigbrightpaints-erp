# Ticket TKT-ERP-STAGE-047

- title: Superadmin Tenant Metrics Baseline Endpoint
- goal: Deliver M18-S2A baseline tenant metrics endpoint (active users + lifecycle) with superadmin-only enforcement
- priority: high
- status: done
- base_branch: tmp/orch-exec-20260217
- created_at: 2026-02-17T12:50:06+00:00
- updated_at: 2026-02-17T13:16:59+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | w1 | merged | `tickets/tkt-erp-stage-047/auth-rbac-company` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec/tickets/TKT-ERP-STAGE-047/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-047`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-047`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-047 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-047 --merge --cleanup-worktrees`
