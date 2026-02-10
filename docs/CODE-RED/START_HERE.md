# CODE-RED: START HERE (Current System Map)

This is the “new agent / new engineer” entrypoint for understanding how the backend works today:
current flows, canonical paths vs aliases, where to find the evidence, and how we ship safely.

CODE-RED rule: document and harden **current behavior** (idempotency, determinism, invariants, locks, deploy safety).
Do not invent new flows without a decision entry + explicit approval.

## 0) One-Command Gates (Always Run Before Claiming “Safe”)

- Local gate (mirrors what CI should do): `bash scripts/verify_local.sh`
  - Runs: schema drift scan + Flyway overlap scan (heuristic) + time API scan + `mvn verify`
  - Release commit mode: `FAIL_ON_FINDINGS=true bash scripts/verify_local.sh`
- Test suite only: `cd erp-domain && mvn -B -ntp verify`
- Staging/prod-like DB predeploy scans (NO-SHIP if any rows):
  - `scripts/db_predeploy_scans.sql` (read-only; must return zero rows)

CI workflow: `.github/workflows/ci.yml` (must remain consistent with `scripts/verify_local.sh`).

## 1) Where To Learn The Current Flows (Authoritative Docs)

High-level map
- Module map: `erp-domain/docs/MODULE_FLOW_MAP.md`
- Cross-module linkage expectations (what must be linked in DB/tests): `erp-domain/docs/CROSS_MODULE_LINKAGE_MATRIX.md`
- Cross-module trace evidence (controllers → services → tables): `docs/cross-module-trace-map.md`
- End-to-end flow (layman + engineering): `docs/CODE-RED/END_TO_END_FLOW.md`

State machines (current behavior only)
- O2C (sales): `erp-domain/docs/ORDER_TO_CASH_STATE_MACHINES.md`
- P2P (purchasing/AP): `erp-domain/docs/PROCURE_TO_PAY_STATE_MACHINES.md`
- Payroll: `erp-domain/docs/HIRE_TO_PAY_STATE_MACHINES.md`

Duplicates / surface area inventories
- Endpoint inventory: `erp-domain/docs/endpoint_inventory.tsv`
- Duplicates report (aliases + Flyway overlaps): `erp-domain/docs/DUPLICATES_REPORT.md`

CODE-RED program docs
- Program plan (detailed): `docs/CODE-RED/plan-v2.md`
- Master plan (short): `docs/CODE-RED/code-red-master-plan.md`
- Team handoff (execution order + next P0 work): `docs/CODE-RED/TEAM_HANDOFF.md`
- Orchestrator contract (strong arm, no parallel truth): `docs/CODE-RED/ORCHESTRATOR_STRONG_ARM_SPEC.md`
- Cross-cutting centralization (duplicate logic → single truth): `docs/CODE-RED/CROSS_CUTTING_CENTRALIZATION.md`
- Identity & naming contract (companyId vs companyCode): `docs/CODE-RED/IDENTITY_AND_NAMING.md`
- Observability identifiers contract (traceId/requestId/idempotency): `docs/CODE-RED/OBSERVABILITY_IDENTIFIERS.md`
- Decision log (what we decided, when): `docs/CODE-RED/decision-log.md`
- Review comments ledger (open findings/questions): `docs/CODE-RED/REVIEW_COMMENTS.md`
- P0 deploy blockers (must fix or prod-gate): `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`
- Module audit summary (1-page snapshot): `docs/CODE-RED/MODULE_AUDIT_SUMMARY.md`
- Performance/query hotspot inventory: `docs/CODE-RED/PERFORMANCE_QUERY_HOTSPOTS.md`
- Release plan / go-no-go / rollback: `docs/CODE-RED/release-plan.md`
- Go / No-Go checklist (single gate): `docs/CODE-RED/GO_NO_GO_CHECKLIST.md`

Database safety / Flyway
- Flyway audit + convergence strategy: `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md`
- Flyway cleanup plan (how we handle 100+ migrations safely): `docs/CODE-RED/FLYWAY_CLEANUP_PLAN.md`

## 2) Canonical Paths (Cheat Sheet)

Dealer onboarding (sales can do this)
- Search: `GET /api/v1/dealers/search` (alias: `GET /api/v1/sales/dealers/search`)
- Create: `POST /api/v1/dealers`
  - Auto-creates dealer AR account (`AR-<dealerCode>`) + portal user (if email not registered).

O2C (order → reserve/slip → dispatch → invoice/journals)
- Create order (“proforma stage”): `POST /api/v1/sales/orders` (uses `dealerId`)
- Dispatch confirm (canonical financial truth): `POST /api/v1/sales/dispatch/confirm`
  - Inventory issue + invoice + AR/COGS journals + dealer ledger sync happen here.
- Dispatch confirm alias: `POST /api/v1/dispatch/confirm` (must behave the same as canonical).

P2P (PO → GRN → purchase invoice → AP settlement)
- Purchase order: `/api/v1/purchasing/purchase-orders/*`
- Goods receipt (GRN): `/api/v1/purchasing/goods-receipts/*` (records stock movements; GL posts at invoice)
- Supplier invoice (raw material purchase): `/api/v1/purchasing/raw-material-purchases/*`
- Supplier settlement/payment: `/api/v1/accounting/settlements/suppliers` and `/api/v1/accounting/suppliers/payments`

Factory (production/packing)
- Production logs: `/api/v1/factory/production/logs/*`
- Packing records: `/api/v1/factory/packing-records/*`
- Bulk packing (bulk → size SKUs): `/api/v1/factory/pack/*`

Orchestrator (cross-module automation)
- Orchestrator endpoints exist, but any path that can set `SHIPPED/DISPATCHED` without canonical dispatch-truth is unsafe.
  Keep orchestrator dispatch/fulfillment gated off until it routes to canonical flows and is idempotent under retries.

## 3) Flyway “Cleanup” (Forward-Only Convergence)

As of **2026-02-02**, this repo has **119** Flyway migrations.

Rules
- Never edit applied migrations.
- Converge drift by adding new “convergence migrations” that declare the final intended schema/constraints.
- Prefer deterministic migrations (avoid `IF NOT EXISTS` drift patterns for converged tables).

Plan reference
- `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md`

## 4) Concrete Deployment Checklist (Summary)

1) Pick release commit and run `bash scripts/verify_local.sh` on that commit.
2) Restore a prod-like dataset into staging (or use a recent staging snapshot).
3) Apply the release and run `scripts/db_predeploy_scans.sql` (NO-SHIP if any rows).
4) Run smoke checks (`erp-domain/scripts/ops_smoke.sh`) and verify health endpoints.
5) Monitor outbox/worker health and error logs after deploy; rollback app first if anything is wrong (no ad-hoc SQL).

Full runbook
- `docs/CODE-RED/release-plan.md`
- `erp-domain/docs/DEPLOY_CHECKLIST.md`
