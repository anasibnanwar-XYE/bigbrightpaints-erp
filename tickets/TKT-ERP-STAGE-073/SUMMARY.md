# Ticket TKT-ERP-STAGE-073

- title: Settlement Replay Guidance Hardening
- goal: Improve fail-closed settlement error contracts with deterministic corrective hints for allocation target conflicts
- priority: high
- status: done
- base_branch: tmp/orch-exec-20260218-active
- created_at: 2026-02-18T08:14:28+00:00
- updated_at: 2026-02-18T08:26:18+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | accounting-domain | w1 | merged | `tickets/tkt-erp-stage-073/accounting-domain` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_active/tickets/TKT-ERP-STAGE-073/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-073`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-073`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-073 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-073 --merge --cleanup-worktrees`
