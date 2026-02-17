# Ticket TKT-ERP-STAGE-030

- title: Async Base Parity Reconciliation
- goal: Resolve Stage-029 full-suite regressions on async-loop-predeploy-audit parity lane
- priority: high
- status: in_review
- base_branch: async-loop-predeploy-audit
- created_at: 2026-02-17T06:39:57+00:00
- updated_at: 2026-02-17T08:35:08+00:00

## Slice Board

| Slice | Agent | Lane | Status | Branch |
| --- | --- | --- | --- | --- |
| SLICE-01 | accounting-domain | w1 | ready | `tickets/tkt-erp-stage-030/accounting-domain` |
| SLICE-02 | purchasing-invoice-p2p | w2 | ready | `tickets/tkt-erp-stage-030/purchasing-invoice-p2p` |
| SLICE-03 | refactor-techdebt-gc | w3 | ready | `tickets/tkt-erp-stage-030/refactor-techdebt-gc` |

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo/tickets/TKT-ERP-STAGE-030/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-030`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-030`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-030 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-030 --merge --cleanup-worktrees`

## Latest Execution Evidence

- strict-lane integration commit pushed on `harness-engineering-orchestrator`: `3e2a36b3`
- full strict harness run passed on integration worktree:
  - `bash ci/check-architecture.sh`
  - `bash ci/check-enterprise-policy.sh`
  - `bash scripts/verify_local.sh` (`Tests run: 1296, Failures: 0, Errors: 0, Skipped: 4`)
- visibility PR created explicitly for app tracking:
  - `https://github.com/anasibnanwar-XYE/bigbrightpaints-erp/pull/15`
- `TKT-ERP-STAGE-001` branch deltas were confirmed already present as patch-equivalent on `harness-engineering-orchestrator`:
  - `84d6332d` (`auth-rbac-company`) -> equivalent
  - `61806bc3` (`sales-domain`) -> equivalent
