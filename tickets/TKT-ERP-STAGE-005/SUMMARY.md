# Ticket TKT-ERP-STAGE-005

- title: ERP Staging Batch 5 - Tenant Hold/Block Runtime Enforcement
- goal: M18-S2A tenant hold/block controls with super-admin authority and fail-closed runtime enforcement
- priority: high
- status: planned
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-16T18:27:11+00:00
- updated_at: 2026-02-16T18:27:11+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | w1 | ready | `tickets/tkt-erp-stage-005/auth-rbac-company` |
| SLICE-02 | refactor-techdebt-gc | w2 | ready | `tickets/tkt-erp-stage-005/refactor-techdebt-gc` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo/tickets/TKT-ERP-STAGE-005/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-005`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-005`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-005 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-005 --merge --cleanup-worktrees`
