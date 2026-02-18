# Ticket TKT-ERP-STAGE-074

- title: Receipt/Payment Duplicate Target Guard
- goal: Extend duplicate-target fail-closed policy to dealer receipt and supplier payment allocation endpoints with deterministic replay-safe diagnostics
- priority: high
- status: planned
- base_branch: tmp/orch-exec-20260218-active
- created_at: 2026-02-18T08:27:28+00:00
- updated_at: 2026-02-18T08:27:28+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | accounting-domain | w1 | ready | `tickets/tkt-erp-stage-074/accounting-domain` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_active/tickets/TKT-ERP-STAGE-074/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-074`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-074`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-074 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-074 --merge --cleanup-worktrees`
