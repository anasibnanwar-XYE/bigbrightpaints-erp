# Ticket TKT-ERP-STAGE-097

- title: Gate Fast Coverage Skip-Blocker Closure
- goal: Eliminate gate_fast release-validation coverage_skipped_files for anchor 06d85e792d2a80cd9fc1f8e5dc15d6dfa15dd93e and establish first-step coverage closure toward threshold pass.
- priority: high
- status: merged
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-20T10:25:45+00:00
- updated_at: 2026-02-20T10:39:42Z

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | w1 | merged | `tickets/tkt-erp-stage-097/auth-rbac-company` |
| SLICE-02 | purchasing-invoice-p2p | w2 | merged | `tickets/tkt-erp-stage-097/purchasing-invoice-p2p` |
| SLICE-03 | reports-admin-portal | w3 | merged | `tickets/tkt-erp-stage-097/reports-admin-portal` |
| SLICE-04 | sales-domain | w4 | merged | `tickets/tkt-erp-stage-097/sales-domain` |
| SLICE-05 | refactor-techdebt-gc | w1 | merged | `tickets/tkt-erp-stage-097/refactor-techdebt-gc` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp/tickets/TKT-ERP-STAGE-097/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-097`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-097`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-097 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-097 --merge --cleanup-worktrees`
