# Task 03 — Auditability + Linkage Contracts (Chain of Evidence + Invariant/Test Mapping)

## Purpose
**Accountant-level:** guarantee every posting has a complete audit trail (who/what/when/why), can be traced end‑to‑end, and reconciles between subledgers and the GL.

**System-level:** define explicit “linkage contracts” for each workflow and map them to enforceable invariants + tests, so the ERP cannot return success while leaving untraceable state.

## Scope guard (explicitly NOT allowed)
- No new business workflows; do not “invent” missing modules.
- Do not relax validations to make tests pass.
- Do not add irreversible financial actions without documented rationale and evidence chain.

## Milestones

### M1 — Write linkage contracts for the core flows (O2C, P2P/AP, Production, Payroll)
Deliverables:
- In this task file, maintain the contracts below:
  - chain of evidence per flow
  - required invariants (fail‑closed)
  - exact tests that enforce each invariant (or mark missing)
- Cross-reference existing anchors:
  - `erp-domain/docs/CROSS_MODULE_LINKAGE_MATRIX.md`
  - `erp-domain/docs/ACCOUNTING_MODEL_AND_POSTING_CONTRACT.md`
  - `erp-domain/docs/RECONCILIATION_CONTRACTS.md`

Verification gates (run after M1):
- `mvn -f erp-domain/pom.xml -DskipTests compile`
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check`
- `mvn -f erp-domain/pom.xml test`

Evidence to capture:
- A “contracts completed” note and a list of any “UNKNOWN/needs verify” items.

Stop conditions + smallest decision needed:
- If a contract requires a link that does not exist in schema: smallest decision is whether to add (A) a forward-only nullable link + backfill plan, or (B) a derived reference mapping (without schema change). Prefer (B) unless auditability requires persisted linkage.

### M2 — Map every invariant to enforcement (tests + runtime guards)
Deliverables:
- For each contract invariant:
  - list the enforcing test(s) (existing or to add)
  - list the enforcing runtime checks (existing or to harden later)
- Create a short “Missing tests to add” register that feeds Task 04/05.

Verification gates (run after M2):
- `mvn -f erp-domain/pom.xml -DskipTests compile`
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check`
- `mvn -f erp-domain/pom.xml test`
- Focused: `mvn -f erp-domain/pom.xml -Dtest=ErpInvariantsSuiteIT test`

Evidence to capture:
- The missing-tests register + prioritized order (highest financial risk first).

Stop conditions + smallest decision needed:
- If enforcing an invariant would break existing intended behavior: smallest decision is whether behavior is actually unintended (bug) vs a documented exception. Default stance: fail‑closed unless exception is explicitly documented and reconciles.

### M3 — Define “evidence chain assertions” (what must be true in DB/API)
Deliverables:
- A set of concrete assertions (API responses and/or SQL checks) that prove linkage:
  - “no posted invoice without journal_entry_id”
  - “no inventory movement of financial type without journal_entry_id”
  - “all dealer/supplier ledger entries reference journal ids and same company”
- These assertions must be runnable as part of later tasks and captured as evidence.

Verification gates (run after M3):
- `mvn -f erp-domain/pom.xml -DskipTests compile`
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check`
- `mvn -f erp-domain/pom.xml test`
- Focused: `mvn -f erp-domain/pom.xml -Dtest=ReconciliationControlsIT,InventoryGlReconciliationIT test`

Evidence to capture:
- The assertion list + sample outputs on a seeded dataset.

Stop conditions + smallest decision needed:
- If assertions require production data access: smallest decision is whether to run on (A) a sanitized prod snapshot, or (B) a prod-like seed dataset; prefer (B) unless investigating a prod incident.

---

## Linkage contracts (maintain as source-of-truth)

### Contract: O2C (Order‑to‑Cash)
Chain of evidence (minimum):
1) Order: `SalesOrder` / `sales_orders`
2) Reservation/dispatch artifact: `PackagingSlip` / `packaging_slips`
3) Inventory issue movements: `InventoryMovement` / `inventory_movements` (and/or RM movements where applicable)
4) Invoice: `Invoice` / `invoices`
5) Journals: `JournalEntry` + `JournalLine` / `journal_entries`, `journal_lines`
6) Dealer subledger: `DealerLedgerEntry` / `dealer_ledger_entries`
7) Settlement/receipt allocations: `PartnerSettlementAllocation` / `partner_settlement_allocations`
8) Reconciliation outputs:
   - dealer statement/aging
   - AR control reconciliation (`RECONCILIATION_CONTRACTS.md`)

Required invariants:
- Dispatch confirm is idempotent: retries do not double-issue stock, double-create invoices, or double-post journals.
- Posted invoice has a journal link (`invoices.journal_entry_id` or equivalent) and is same-company.
- Inventory issue movements created by dispatch are linked to their posting journals when posting is expected.
- Dealer ledger entries reference the same invoice/journal identifiers used by accounting and are same-company.
- Settlement allocations are idempotent and cannot exceed outstanding (no negative outstanding drift).

Existing tests (verify/extend as needed):
- `ErpInvariantsSuiteIT` (golden O2C + linkage + idempotency)
- `OrderFulfillmentE2ETest`, `DispatchConfirmationIT`, `DealerLedgerIT`, `SettlementE2ETest`, `GstInclusiveRoundingIT`

Gaps to verify (do not assume):
- Any “unallocated receipt” endpoints that post without allocations (must not drift subledger).
- Any alias endpoints that bypass intended RBAC.

### Contract: P2P/AP (Procure‑to‑Pay / Accounts Payable)
Chain of evidence (minimum):
1) Supplier: `Supplier` / `suppliers`
2) Purchase: `RawMaterialPurchase` / `raw_material_purchases`
3) Receipt movements/batches: `RawMaterialMovement` + `RawMaterialBatch` / `raw_material_movements`, `raw_material_batches`
4) Journals: `JournalEntry`/`JournalLine`
5) Supplier subledger: `SupplierLedgerEntry`
6) Settlement/payment allocations: `PartnerSettlementAllocation`
7) Reconciliation outputs:
   - supplier statement/aging
   - AP control reconciliation

Required invariants:
- Purchase posts AP + inventory effects and is linked to journals (same-company).
- Receipt movements exist, are ordered/idempotent, and link to journals when posting is expected.
- Supplier settlements/payments are idempotent and reconcile to AP control account within tolerance.
- Purchase returns reverse inventory and AP consistently and are traceable.

Existing tests (verify/extend as needed):
- `ErpInvariantsSuiteIT` (golden P2P)
- `ProcureToPayE2ETest`, `SupplierStatementAgingIT`, `ReconciliationControlsIT`

### Contract: Production (Produce‑to‑Stock)
Chain of evidence (minimum):
1) Production log: `ProductionLog` / `production_logs`
2) Production materials: `production_log_materials`
3) Packing record: `PackingRecord` / `packing_records`
4) Finished goods batch: `FinishedGoodBatch` / `finished_good_batches`
5) Inventory movements (RM consumption + FG creation): movements tables
6) Journals (if costing/WIP enabled): `JournalEntry`/`JournalLine`
7) Reconciliation outputs:
   - inventory valuation/reconciliation
   - WIP/FG rollforward (if applicable)

Required invariants:
- No orphan production logs/packing records: each creates the expected movements/batches.
- Stock movements from production are linked back to production references (traceability) and same-company.
- If costing journals are posted, they are linked to production references and balanced.

Existing tests (verify/extend as needed):
- `ErpInvariantsSuiteIT` (production golden path)
- `FactoryPackagingCostingIT`, `CompleteProductionCycleTest`, `WipToFinishedCostIT`

### Contract: Payroll (Hire‑to‑Pay)
Chain of evidence (minimum):
1) Payroll run: `PayrollRun` / `payroll_runs`
2) Payroll lines/totals: payroll line tables (verify actual names)
3) Posting journal: `JournalEntry`/`JournalLine` linked to payroll run
4) Payment marking / batch payments: payroll payment artifacts (verify)
5) Reconciliation outputs:
   - payroll expense vs payable clearing
   - period close impact

Required invariants:
- Payroll run state machine is enforced: calculate → approve → post → mark-paid (no skipping).
- Posting produces a linked journal entry; reversals are balanced inverse.
- Advances/withholdings clearing is consistent between payroll math and posting.
- Mark-paid operations are idempotent and auditable.

Existing tests (verify/extend as needed):
- `ErpInvariantsSuiteIT` (hire-to-pay golden + reversal)
- `PayrollBatchPaymentIT`, `PeriodCloseLockIT`

