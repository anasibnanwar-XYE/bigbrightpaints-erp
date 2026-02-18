# Ticket TKT-ERP-STAGE-075

- title: Accounting Report Export Governance
- goal: Enforce admin-only accounting export policy with mandatory audit logging and dealer invoice/ledger boundary checks
- priority: high
- status: planned
- base_branch: async-loop-predeploy-audit
- created_at: 2026-02-18T10:30:17+00:00
- updated_at: 2026-02-18T10:30:17+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | accounting-domain | w1 | ready | `tickets/tkt-erp-stage-075/accounting-domain` |
| SLICE-02 | purchasing-invoice-p2p | w2 | ready | `tickets/tkt-erp-stage-075/purchasing-invoice-p2p` |
| SLICE-03 | sales-domain | w3 | ready | `tickets/tkt-erp-stage-075/sales-domain` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp/tickets/TKT-ERP-STAGE-075/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-075`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-075`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-075 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-075 --merge --cleanup-worktrees`
