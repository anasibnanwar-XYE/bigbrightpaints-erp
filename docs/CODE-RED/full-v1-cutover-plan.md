# CODE-RED Full V1 Cutover Plan (Plan B)

Audience: backend + QA + DevOps. This is a "do-the-work" plan, not a vision doc.

Scope focus: Manufacturing/Packaging canonicalization (variants + BOM), idempotency, and posting boundaries, while
preserving existing workflows (no silent behavior changes; fail-closed on ambiguity).

Important: We cannot promise "nothing will break" in production. What we *can* promise is:
- every change is gated by automated tests + predeploy scans;
- data migrations are deterministic and reversible;
- rollout is staged with explicit rollback steps;
- ambiguous behavior fails closed instead of corrupting inventory/ledger.

Repo/worktree location:
- `/home/realnigga/Desktop/CLI_BACKEND_epic04`

Primary reference docs:
- Canonical packaging model: `docs/CODE-RED/packaging-flow.md` (target spec)
- Stabilization epics: `docs/CODE-RED/stabilization-plan.md` (milestones/status)
- Release gates: `docs/CODE-RED/release-plan.md`

## 0) Non-Negotiables (CODE-RED Law)

1) Posting boundary
- All journals must be posted via `AccountingFacade` (or a facade-owned method) for correctness + idempotency.
- Any direct `AccountingService.createJournalEntry` call from non-accounting modules is a CODE-RED violation.

2) Idempotency
- Every "business event" that mutates stock and/or journals must be idempotent at the correct boundary:
  - packing conversion idempotent per (company, requestIdempotencyKey)
  - journal reference unique per business event
  - inventory movements deduped by deterministic reference

3) Fail-closed on ambiguity
- If we cannot prove which slip/batch/event is correct, we reject the request and require an explicit identifier.

4) No schema drift
- New migrations must be deterministic and avoid "IF NOT EXISTS" patterns (convergence only once we're stable).

## 1) Definition of Done (Full V1)

We consider "Full V1 cutover done" only when:
- Packaging variants + BOM schema exists and is enforced for packing (no legacy fallback).
- Bulk-to-size packing is deterministic + idempotent, and consumes BOM materials (stock + movements) atomically.
- Packing journals route via `AccountingFacade` and are period-locked + reference-unique.
- Predeploy scans return zero rows on staging data: `scripts/db_predeploy_scans.sql`.
- `bash scripts/verify_local.sh` passes on the release commit.

## 2) Current Reality (Repo-Verified Gaps We Must Fix)

These are hard blockers for "Full V1":

A) Variant/BOM schema does not exist
- `product_packaging_variants`, `product_packaging_components` tables are referenced in docs but not in migrations.
- Current implementation uses legacy *global* packaging size mappings (`V69__packaging_size_mappings_and_gst_inventory.sql`,
  `V99__packaging_size_mapping_components.sql`) and global `packagingSize` strings.

B) Packing bypasses the posting boundary
- `PackingService` and `BulkPackingService` post journals directly via `AccountingService.createJournalEntry(...)`.

C) Bulk packing is not idempotent at the API boundary
- `BulkPackRequest` has no request idempotency key.
- References are not reserved-first; retries can create duplicates under failure/retry conditions.

D) Deployment safety blockers outside packing still exist
- Mutating/nondeterministic slip lookup: `/api/v1/dispatch/order/{orderId}` (creates slips + selects "most recent").
- Orchestrator can mark SHIPPED/DISPATCHED without canonical dispatch (bypasses invariants).

Full V1 cutover cannot ship while D remains unaddressed (even if packing is perfect).

## 3) Architecture / Ownership (What Owns What)

Manufacturing and packing touches 3 truths:
- Operational: packing record / production log status
- Inventory: movements + batches + current_stock
- Accounting: journals

Canonical ownership rules:
- Factory modules own operational docs (production log, packing event).
- Inventory modules own stock/movement ledgers.
- Accounting owns journals; factory/inventory never "direct post" outside `AccountingFacade`.

## 4) Work Breakdown (Epics -> Milestones -> Tasks)

This plan uses EPIC IDs aligned with `docs/CODE-RED/stabilization-plan.md`, but expands tasks + tests for execution.

### EPIC 01 (Hardening prerequisite): Stop non-canonical dispatch pathways

Goal: prevent data corruption while we change packing.

M01.B1 - Make slip lookup deterministic and non-mutating
- Change `/api/v1/dispatch/order/{orderId}` to be read-only:
  - If slips exist:
    - if exactly 1 -> return it
    - if >1 -> fail closed with "provide slipId"
  - If no slips exist -> do NOT create one on GET; return 404/validation error
- Remove internal "select most recent slip" usage for any externally callable lookup path.

Tests
- Add test: GET slip by orderId must not create slip/reservation (no side effects).
- Add test: multiple slips -> orderId lookup fails closed.

M01.B2 - Orchestrator cannot set SHIPPED/DISPATCHED without canonical dispatch
- Hard-block the orchestrator status transition to SHIPPED/DISPATCHED unless routed through the canonical dispatch
  workflow (`SalesService.confirmDispatch`).
- If we must keep the endpoint for integrations, allow only statuses up to READY_TO_SHIP.

Tests
- Add integration test: orchestrator SHIPPED/DISPATCHED request is rejected when no dispatched slip/journals exist.

Acceptance criteria
- No endpoint exists that can produce "order shipped" state without slip dispatch + invoice/journals.

---

### EPIC 03 (Core): Packaging variants + BOM + packing idempotency + AccountingFacade-only posting

Goal: enforce the model in `docs/CODE-RED/packaging-flow.md` as a hard cutover without losing operational capability.

#### M03.0 - Add Variant/BOM Schema (Additive + Backfilled)

Deliverable: New schema exists, is populated from legacy mappings, and is queryable by services.

DB migrations (new Flyway versions)
1) Create tables (additive)
- `product_packaging_variants`
  - columns (suggested):
    - `id` PK
    - `company_id` FK -> companies
    - `production_product_id` FK -> production_products
    - `size_label` (e.g., "1L", "5L")
    - `liters_per_unit` numeric(12,4)
    - `child_finished_good_id` FK -> finished_goods
    - `active` boolean default true
    - audit cols if standard in schema
  - constraints:
    - `uk_variant_company_product_size` unique (company_id, production_product_id, size_label)
    - optional: `uk_variant_company_child_sku` unique (company_id, child_finished_good_id)

- `product_packaging_components`
  - columns (suggested):
    - `id` PK
    - `company_id` FK -> companies
    - `variant_id` FK -> product_packaging_variants
    - `raw_material_id` FK -> raw_materials
    - `units_per_pack` numeric(12,4)
    - `active` boolean default true
  - constraints:
    - `uk_component_company_variant_material` unique (company_id, variant_id, raw_material_id)

2) Backfill variants and BOM from legacy tables
- Source tables today:
  - `packaging_size_mappings` (created in `V69__packaging_size_mappings_and_gst_inventory.sql`)
    - NOTE: this table is global by (company, packaging_size, raw_material); it is *not* per product today.
    - `V99__packaging_size_mapping_components.sql` adjusts constraints/indexes on the same table (it does not introduce a
      separate components table).
- Backfill rules (deterministic, but may require manual follow-up if naming conventions are inconsistent):
  - Create variants per (production_product, size SKU):
    - For each `production_products.sku_code` (base code), find child size SKUs in `finished_goods.product_code` by naming
      convention: `<sku_code>-<sizeLabel>` (example: `SAFARI-WHITE-1L`).
    - Derive `size_label` from the suffix after `<sku_code>-`.
    - Set `liters_per_unit` from `packaging_size_mappings.liters_per_unit` for that `size_label` (fail closed if missing).
    - Set `child_finished_good_id` to the matched finished_good row.
  - Create BOM components per (variant):
    - For each variant.size_label, copy all `packaging_size_mappings` rows for (company, packaging_size=size_label) into
      `product_packaging_components` as the default BOM (raw_material_id + units_per_pack [+ carton_size if needed]).
  - Any production product that has no matching size SKUs (or any SKU that does not follow naming conventions) must be
    manually configured before cutover; the predeploy scans must surface these cases explicitly.

3) Add "configuration completeness" scan queries
- Add queries to `scripts/db_predeploy_scans.sql`:
  - active production products with no variants
  - active variants with liters_per_unit null/<=0
  - active variants with zero BOM components (if required)
  - variants pointing to finished_goods of another company (tenant isolation)

Tests
- Migration validation via Testcontainers (already part of suite); add targeted test if needed.

Acceptance criteria
- Schema exists + backfill runs on fresh DB.
- Predeploy scan reports missing config rather than allowing packing to proceed.

#### M03.1 - Introduce Domain Services for Variants/BOM (Read Path)

Deliverables
- Add repositories + services:
  - `ProductPackagingVariantRepository`
  - `ProductPackagingComponentRepository`
  - `PackagingVariantService` (read/write)
  - `PackagingBomResolver` (compute required materials from packs)

Key rules enforced in code
- liters_per_unit > 0
- units_per_pack >= 0
- Discrete material units must be whole numbers (PCS/EA/UNIT), else fail closed.

API (admin-only)
- Add endpoints to manage variants and components (or repurpose legacy mapping endpoints as wrappers):
  - CRUD variant
  - CRUD BOM components
  - Preview endpoint: given bulkBatchId + pack lines => returns:
    - required liters
    - required packaging materials
    - validation errors (stock insufficient, variant missing, BOM missing)

Tests
- Unit tests for BOM resolver math + rounding.
- Integration test: preview fails closed when variant/BOM missing.

Acceptance criteria
- The system can compute BOM requirements deterministically from pack request.

#### M03.2 - Bulk Packing Becomes Canonical (Idempotent + BOM + Movements + Posting)

Deliverables
1) Request idempotency
- Add `idempotencyKey` to `BulkPackRequest` (required).
- Add DB uniqueness for pack events:
  - Option A: new `packing_events` table with (company_id, idempotency_key) unique.
  - Option B: use `journal_reference_mappings` reserve-first for packing references (acceptable if strictly controlled).

2) Deterministic reference namespace
- Define reference format:
  - `PACK-<bulkBatchCode>-<seq>` OR `PACK-<hash(idempotencyKey)>`
  - Must be unique per company and stable across retries.
- All inventory movements and raw material movements use the same reference id.

3) Enforce BOM consumption
- Server computes packaging material requirements from variants/BOM (do not trust client-provided totals).
- Consume raw materials atomically (lock batches, update current_stock, insert raw_material_movements).

4) Inventory movements
- Issue bulk liters from the selected bulk batch (movement type ISSUE).
- Receipt child batches for each size SKU (movement type RECEIPT).
- Preserve parent batch reference in child batches.

5) Accounting via AccountingFacade
- Add `AccountingFacade.postPackingConversion(...)`:
  - Dr Finished Goods Inventory (size SKUs total value)
  - Cr Bulk Inventory (bulk component)
  - Cr Packaging RM Inventory (packaging component)
  - Delta/rounding -> Packing Variance (explicit)
- Ensure period lock enforced (use entryDate/business date).
- Ensure journal reference uniqueness is the idempotency gate for posting.

6) Remove/lock down `skipPackagingConsumption`
- Full V1 target: disallow skip in production.
- If the UI currently relies on a two-step workflow, implement an explicit two-step contract instead of a boolean:
  - Step A: "reserve/consume packaging materials" event with idempotency key
  - Step B: "convert bulk to sizes" referencing the same event id
  - This preserves functionality without double consumption.

Tests (must add)
- CR_BulkPackingVariantRequiredIT: missing variant => fails closed.
- CR_BulkPackingBomRequiredIT: missing BOM => fails closed.
- CR_BulkPackingIdempotencyIT:
  - send same request twice -> same pack reference, no extra movements, same journal id
- CR_BulkPackingConcurrencyIT:
  - N concurrent requests with same idempotencyKey -> exactly one pack event created
- CR_BulkPackingCostingIT:
  - verify child unit costs == (bulk liters cost + BOM packaging cost) / units, with variance posted explicitly

Acceptance criteria
- A single pack request produces:
  - exactly one pack event
  - deterministic movements
  - deterministic journal
  - correct stock deltas

#### M03.3 - ProductionLog Packing Flow Aligns (No Posting Bypass)

Goal: keep production log workflows working without creating a second packing semantics.

Decision needed (choose one, document in decision log):
Option 1 (recommended): ProductionLogService produces bulk only; BulkPackingService does all size packing.
- `PackingService.recordPacking` is deprecated/disabled, replaced by bulk packing.
- Production completion is tracked on ProductionLog; packing history moves to pack events.

Option 2 (compat): Keep `PackingService` but make it a pure operational layer.
- It must NOT:
  - post journals via AccountingService
  - consume packaging via legacy global mapping
  - create "fake bulk batches" with packaging cost embedded
- It may:
  - record operator actions / QC notes
  - call BulkPackingService with explicit variants (but then it becomes a wrapper, not a second algorithm)

Regardless of option:
- Remove direct `AccountingService.createJournalEntry` usage from factory services.
- Any posting required must be done via `AccountingFacade`.

Tests
- Static enforcement test/gate:
  - fail if any non-accounting package calls `AccountingService.createJournalEntry`.
- Integration tests updated to reflect the chosen flow.

Acceptance criteria
- There is exactly one algorithm for bulk->size conversion and BOM consumption (not two competing paths).

#### M03.4 - Deprecate Legacy Mapping (Hard Cutover)

Deliverables
- Packing services must read variants/BOM only.
- Legacy mapping endpoints:
  - either removed, or rewritten to write-through to the new tables for admin convenience.
- Add a runtime guard:
  - `erp.packaging.require-variants-bom=true` in prod.
- Remove any "legacy fallback" flags like `erp.benchmark.require-packaging=false` for packing.

Tests
- Ensure packing fails closed if variants/BOM missing even if legacy mappings exist.

Acceptance criteria
- No production path depends on packaging_size_mappings for packing.

---

### EPIC 08 (Required for confidence): Schema Convergence + Drift Elimination

Goal: reduce environment drift so deploys behave consistently.

M08.B1 - Identify drift-heavy tables
- Use `scripts/schema_drift_scan.sh` output to identify drift generators (IF NOT EXISTS, ALTER IF EXISTS, etc).
- Prioritize tables that affect packing and posting:
  - inventory movements
  - packing records
  - journal reference mappings
  - payroll runs (historical drift)

M08.B2 - Convergence migrations
- Create a single convergence migration per domain area:
  - define the true schema state
  - backfill deterministically
  - add constraints
- Once converged:
  - enable `FAIL_ON_FINDINGS=true` for schema drift scan in CI (future guard).

Acceptance criteria
- Fresh install schema matches prod schema for the converged areas.

## 5) Test Plan (How We Prove We Didn't Break Core ERP Truth)

Mandatory commands (release candidate)
- `bash scripts/verify_local.sh`
- Optional (faster CODE-RED suite): `cd erp-domain && mvn -B -ntp -Pcodered test`

Required new automated coverage (packing cutover)
- Variant/BOM enforcement tests (fail closed)
- Bulk packing idempotency + concurrency
- Bulk packing inventory movement correctness
- Bulk packing journal correctness + variance policy
- Cross-module smoke: bulk pack -> sales dispatch -> COGS uses packed cost

Predeploy scans
- Run `scripts/db_predeploy_scans.sql` on staging dataset; must return zero rows.
- Add new queries for missing variants/BOM and broken pack links.

## 6) Rollout Plan (Staged, Reversible)

Stage 0: Prepare
- Deploy code with new schema + backfill migrations (variants/BOM tables populated).
- Keep runtime guard OFF initially (`require-variants-bom=false`) but run scans to verify config completeness.

Stage 1: Shadow validation
- In staging, enable guard ON and run:
  - packing preview + packing execution tests
  - end-to-end flow (production -> pack -> dispatch)
- Fix any missing BOM/variant configuration.

Stage 2: Production cutover
- Enable guard ON in prod.
- Monitor:
  - pack event rate
  - raw material negative stock rejections
  - duplicate journal reference errors
  - dispatch failure rate

Rollback (if needed)
- Disable guard (revert to "no packing") only if you have a safe fallback.
- Prefer application rollback to last known-good build.
- Do NOT run manual SQL edits under pressure; use audited repair flows only.

## 7) Risk Register (Top Risks + Mitigations)

1) Data migration ambiguity (legacy mapping missing liters_per_unit)
- Mitigation: fail-closed scans; require explicit data fix before enabling guard.

2) Double consumption of packaging materials
- Mitigation: reserve-first idempotency; single canonical pack event; explicit two-step contract if needed.

3) Duplicate journals/movements under retries
- Mitigation: deterministic references + DB unique constraints; idempotencyKey required.

4) Performance regressions (packing touches many tables)
- Mitigation: batch locks + indexes; keep pack queries bounded; add profiling on staging.

5) Client/UI mismatch (API contract changes)
- Mitigation: provide preview endpoint; keep backward-compatible wrapper endpoints short-term if required; communicate cutover date.
