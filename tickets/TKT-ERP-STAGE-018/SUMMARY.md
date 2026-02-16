# Ticket TKT-ERP-STAGE-018

- title: ERP Staging Batch 18 - GST Boundary Guard
- goal: M18-S6B tighten GST/non-GST boundary behavior with deterministic accounting-safe validation
- priority: high
- status: done
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-16T22:13:55+00:00
- updated_at: 2026-02-16T22:25:44+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | accounting-domain | w1 | merged | `tickets/tkt-erp-stage-018/accounting-domain` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo/tickets/TKT-ERP-STAGE-018/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-018`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-018`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-018 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-018 --merge --cleanup-worktrees`
