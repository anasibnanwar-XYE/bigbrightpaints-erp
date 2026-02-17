# Ticket TKT-ERP-STAGE-046

- title: Superadmin Tenant Metrics Baseline Endpoint
- goal: Deliver M18-S2A baseline tenant metrics endpoint (active users + lifecycle) with superadmin-only enforcement
- priority: high
- status: canceled
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-17T12:48:46+00:00
- updated_at: 2026-02-17T13:18:00+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | w1 | canceled | `tickets/tkt-erp-stage-046/auth-rbac-company` |
| SLICE-02 | refactor-techdebt-gc | w2 | canceled | `tickets/tkt-erp-stage-046/refactor-techdebt-gc` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec/tickets/TKT-ERP-STAGE-046/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-046`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-046`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-046 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-046 --merge --cleanup-worktrees`
