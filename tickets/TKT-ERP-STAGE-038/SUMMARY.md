# Ticket TKT-ERP-STAGE-038

- title: Flake Quarantine Contract Tightening
- goal: Enforce expiring quarantine policy and keep release decisions based on invariant signal quality
- priority: high
- status: completed
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-17T09:55:06+00:00
- updated_at: 2026-02-17T10:33:00Z

## Slice Board

| Slice | Agent | Lane | Status | Branch | Source Commit | Integrated Commit |
| --- | --- | --- | --- | --- |
| SLICE-01 | release-ops | w1 | merged | `tickets/tkt-erp-stage-038/release-ops` | `9676dfca` | `0d77fffd` |
| SLICE-02 | repo-cartographer | w2 | merged | `tickets/tkt-erp-stage-038/repo-cartographer` | `7b0314bf` | `586a12ed` |

## Closure Evidence

- Required gate ladder completed on integration SHA `0d77fffdac006b67b77a1ef58cb30c4afcf0e194`:
  - `bash ci/lint-knowledgebase.sh` -> PASS
  - `bash ci/check-architecture.sh` -> PASS
  - `bash ci/check-enterprise-policy.sh` -> PASS
  - `PGHOST=127.0.0.1 PGPORT=55432 PGUSER=erp PGPASSWORD=erp PGDATABASE=postgres bash scripts/gate_reconciliation.sh` -> PASS
  - `PGHOST=127.0.0.1 PGPORT=55432 PGUSER=erp PGPASSWORD=erp PGDATABASE=postgres bash scripts/gate_release.sh` -> PASS
  - `bash scripts/verify_local.sh` -> PASS (`Tests run: 1296, Failures: 0, Errors: 0, Skipped: 4`)
- SLICE-01 fail-closed enforcement is active: quarantine entries now require valid expiry metadata and expired/malformed entries fail gates.
- SLICE-02 policy docs are aligned with runtime guard behavior and release signal-quality expectations.

## Operator Commands

Read cross-workflow dependency plan:
`cat /home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_harness_integrate/tickets/TKT-ERP-STAGE-038/CROSS_WORKFLOW_PLAN.md`

Generate tmux launch block:
`python3 scripts/harness_orchestrator.py dispatch --ticket-id TKT-ERP-STAGE-038`

Verify / readiness pass:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-038`

Verify + merge eligible slices:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-038 --merge`

Verify + merge + cleanup worktrees:
`python3 scripts/harness_orchestrator.py verify --ticket-id TKT-ERP-STAGE-038 --merge --cleanup-worktrees`
