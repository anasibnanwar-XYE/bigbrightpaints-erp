# Ticket TKT-ERP-STAGE-050

- title: Tenant Metrics Observability Envelope Stage-1
- goal: M18-S2A: extend tenant metrics payload with deterministic API activity and error-rate counters for superadmin control-plane observability
- priority: high
- status: planned
- base_branch: tmp/orch-exec-20260217
- created_at: 2026-02-17T13:31:25+00:00
- updated_at: 2026-02-17T13:31:25+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | w1 | ready | `tickets/tkt-erp-stage-050/auth-rbac-company` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec/tickets/TKT-ERP-STAGE-050/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-050`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-050`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-050 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-050 --merge --cleanup-worktrees`
