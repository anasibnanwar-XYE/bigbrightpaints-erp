# Ticket TKT-ERP-STAGE-007

- title: ERP Staging Batch 7 - M18-S6A GST/Non-GST Drift Guards
- goal: M18-S6A smallest shippable closure: enforce GST/non-GST settlement posting drift guards and reconciliation-safe contracts
- priority: high
- status: merged
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-16T19:15:38+00:00
- updated_at: 2026-02-17T01:47:00+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | accounting-domain | w1 | merged | `tickets/tkt-erp-stage-007/accounting-domain` |
| SLICE-02 | purchasing-invoice-p2p | w2 | merged | `tickets/tkt-erp-stage-007/purchasing-invoice-p2p` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo/tickets/TKT-ERP-STAGE-007/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-007`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-007`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-007 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-007 --merge --cleanup-worktrees`
