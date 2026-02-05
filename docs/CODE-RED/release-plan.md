# CODE-RED Release Plan (V1)

This is the PM + senior-dev execution plan for getting the backend deployable under CODE-RED constraints.
It is intentionally scoped to "stability, invariants, and repeatable deploys" (not features).

For the detailed module-by-module execution plan, see:
- `docs/CODE-RED/plan-v2.md` (full detailed plan)
- `docs/CODE-RED/code-red-master-plan.md` (shorter master plan)
- Flyway audit + convergence strategy: `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md`

## 0) Goal / Definition of Done

We are "deployable" when all are true:
- We can deploy repeatedly without ad-hoc database edits.
- Retrying any write action (double-click / network retry / orchestrator retry) does not create duplicate documents, stock
  movements, or journal entries (idempotency at the correct boundaries).
- Operational truth (orders, slips, packing) does not contradict financial truth (journals) for the same business event.
- The pre-deploy scans return clean on a production-like dataset.

## 1) Current Status Snapshot (as of 2026-02-05)

Verified green gates:
- Local release gate passes: `bash scripts/verify_local.sh` (schema drift scan + Flyway overlap scan + time API scan + `mvn verify`).
- CI must execute the same gate as `scripts/verify_local.sh` (see `.github/workflows/ci.yml`).

Known open risks (must be explicitly handled before prod use):
- Mutating/nondeterministic GET: `/api/v1/dispatch/order/{orderId}` can create slips/reservations and selects "most recent"
  slip when multiple exist.
- Orchestrator can mark orders SHIPPED/DISPATCHED without canonical dispatch (bypasses slip/invoice/journal invariants).
- Packing posts journals directly via `AccountingService` (bypasses `AccountingFacade`) and bulk packing is not idempotent
  at the API boundary.
- Packaging hard-cutover schema (variants + BOM) is not implemented; current implementation uses legacy size mappings.
- `AccountingEventStore` exists but is explicitly **not** relied upon for temporal truth; closed-period reporting must use period-end snapshots.
- Period-end snapshots are a **decision** but not yet fully implemented; until EPIC 06 is complete, closed-period reports can drift.

P0 deploy blockers reference:
- `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`

## 2) Release Strategy (How We Ship Without Breaking Accounting/Inventory)

CODE-RED reality: if we cannot finish a full canonicalization epic safely, we ship by reducing the blast radius.

Two allowed release modes:
1) "Safe subset" release (recommended for immediate deploy)
   - Disable/lock down unsafe endpoints/paths so only canonical flows are used.
   - Keep manufacturing/packing either disabled in prod, or behind a strict allowlist until EPIC 03 (Scope A hardening) is complete.
   - Disable any “silent drift” automation (inventory→GL listeners) unless it is enterprise-grade (durable + observable + exactly-once).
2) "Full V1" release (requires EPIC 03 + EPIC 08 completion)
   - Implement per-product packaging variants + BOM hard cutover.
   - Route all manufacturing postings through `AccountingFacade`.
   - Convergence migrations remove schema drift patterns.
   - Detailed execution plan: `docs/CODE-RED/full-v1-cutover-plan.md`.

## 3) Workstreams (Epics) and Concrete Deliverables

This section is an execution view of `docs/CODE-RED/stabilization-plan.md`, with added delivery artifacts and owners.

EPIC 00 - Discipline / Gates
- Deliverables:
  - CI: schema drift scan + time API scan + `mvn verify` + triage on failure (already live).
  - Local: `scripts/verify_local.sh` remains the "one-command" gate (already live).
  - Release artifacts: a "Go/No-Go" checklist and rollback runbook (see Sections 5 and 6).

EPIC 01 - Sales -> Inventory -> Dispatch -> Invoice -> Accounting (Dispatch-Truth)
- Deliverables:
  - Single canonical dispatch path (`SalesService.confirmDispatch`) remains the only state-changing dispatch.
  - Remove/disable ambiguous alias endpoints where they can bypass invariants.
  - Block/replace mutating GET slip lookup by orderId (must be read-only or fail-closed on ambiguity).

EPIC 02 - Purchasing / Supplier Intake
- Deliverables:
  - Supplier invoice posting idempotency + uniqueness enforcement (no duplicates on retry).
  - Period close continues to block on un-invoiced GRNs (already enforced by tests).

EPIC 03 - Manufacturing/WIP + Packaging Canonicalization (Bulk -> Size SKUs)
- Deliverables (Full V1 mode):
  - Implement packaging variants + BOM tables and hard-cutover logic.
  - Bulk packing and packing records become idempotent (reserve-first).
  - All packing journals route via `AccountingFacade` (no direct `AccountingService.createJournalEntry` calls).
- Deliverables (Safe subset mode):
  - Disable/lock down packing endpoints in prod until safety hardening is complete (idempotency + deterministic references + facade-owned posting).
  - Acceptance criteria (safe subset): any disabled endpoint must return a clear `canonicalPath` and must be covered by an integration test.

EPIC 04 - Payroll Safety (Single Path, Idempotent)
- Deliverables:
  - Payroll run creation is idempotent per (company + runType + period) and concurrency-safe.
  - Payroll payment posting is idempotent and cannot double-pay under retries/concurrency.
  - Orchestrator payroll remains disabled until it routes to canonical HR + Accounting flows (no legacy “fast pay” bypass).

EPIC 05 - Manual Journal Policy + Period Locks
- Deliverables:
  - Manual journal references are system-generated only (already live).
  - Manual journal idempotency is reserve-first (already live).
  - Add/require memo/reason for manual journals (pending).
  - Remove/demote `AccountingEventStore` temporal-truth claims (snapshots + journals are truth).

EPIC 06 - Period Close + As-Of Correctness (No Silent Drift)
- Deliverables:
  - As-of strategy is **snapshots**: persist period-end snapshots at close so closed periods cannot drift when “today” changes.
  - Reporting must read snapshots for CLOSED periods; `AccountingEventStore` is not used for temporal truth.
  - Add a regression test: close period A, post in period B, verify period A reconciliation remains unchanged.

EPIC 07 - Inventory→GL Enterprise-Grade Automation (Or Disabled)
- Deliverables:
  - Inventory→GL automation must not swallow failures silently.
  - If enabled, it must be durable (outbox + retry), observable (health + metrics), and exactly-once.
  - If not yet enterprise-grade, keep it disabled in production (safe subset mode).

EPIC 08 - Schema Convergence / Drift Elimination
- Deliverables:
  - Convergence migrations for drift-heavy tables (payroll, packing, journals, etc.).
  - Enable FAIL_ON_FINDINGS for drift scan in CI only after convergence is complete.

EPIC 09 - Deploy Gates + Observability + Rollback
- Deliverables:
  - Pre-deploy scan gate is enforced on staging/prod datasets: `scripts/db_predeploy_scans.sql` must return zero rows.
  - Rollback plan is documented and rehearsed (app rollback + "no manual data edits" discipline).

## 4) Milestone Plan (Suggested Sequence)

If the priority is "deploy soon", do this order:
1) RC0 (Stop-the-bleeding)
   - Disable orchestrator fulfillment status updates in prod (or route to canonical dispatch).
   - Make `/api/v1/dispatch/order/{orderId}` read-only or fail-closed (no side effects; no "most recent" selection).
   - Decide whether packing is enabled in prod:
     - If enabled: EPIC 03 partial completion is required (AccountingFacade + idempotency).
     - If not enabled: explicitly disable/guard endpoints.
2) RC1 (Hardening)
   - Supplier invoice idempotency (EPIC 02).
   - Manual journal memo requirement + event store decision (EPIC 05).
3) V1 Full Cutover (Longer tail)
   - EPIC 03 packaging variants + BOM hard cutover.
   - EPIC 08 convergence migrations; then enforce drift scan failures in CI.

## 5) Go / No-Go Checklist (Deploy Gate)

Authoritative gate: `docs/CODE-RED/GO_NO_GO_CHECKLIST.md`

## 6) Release Runbook (Authoritative)

Authoritative release procedure (commands, expected outputs, and failure criteria):
`docs/CODE-RED/RELEASE_RUNBOOK.md`

Reference-only checklist (env vars + endpoints):
`erp-domain/docs/DEPLOY_CHECKLIST.md`

## 7) Ownership (RACI-lite)

- PM/Release Captain: owns scope, timelines, go/no-go, comms.
- Backend Lead: owns invariants, canonical workflow enforcement, and merges.
- QA/Verifier: owns running gates on staging/prod-like data and signing off on scans.
- DevOps: owns deploy automation, rollback mechanics, observability dashboards/alerts.
