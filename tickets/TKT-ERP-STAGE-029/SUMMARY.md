# Ticket TKT-ERP-STAGE-029

- title: ERP Staging Batch 29
- goal: Sync Stage-028 evidence into slice branches for merge eligibility
- priority: high
- status: planned
- base_branch: async-loop-predeploy-audit
- created_at: 2026-02-17T06:00:50+00:00
- updated_at: 2026-02-17T06:00:50+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | orchestrator | w1 | ready | `tickets/tkt-erp-stage-029/orchestrator` |
| SLICE-02 | refactor-techdebt-gc | w2 | ready | `tickets/tkt-erp-stage-029/refactor-techdebt-gc` |
| SLICE-03 | repo-cartographer | w3 | ready | `tickets/tkt-erp-stage-029/repo-cartographer` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo/tickets/TKT-ERP-STAGE-029/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-029`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-029`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-029 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-029 --merge --cleanup-worktrees`
