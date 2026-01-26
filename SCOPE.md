# SCOPE — Pre‑Deploy Blockers Remediation (Inventory / Accounting / Integration / Security)

## Goal
Stabilize the ERP for production by fixing pre-deploy blockers across inventory reservations/dispatch, accounting postings, integration/orchestrator flows, and session security — without introducing new business features.

## Definition of Done (DoD)
- All critical flows produce balanced journal entries (Σdebits == Σcredits with zero tolerance at posting time).
- Dispatch confirmation produces exactly one AR/Revenue/Tax posting and one COGS/Inventory relief posting per dispatch, and is safe to retry (idempotent) across both dispatch entrypoints:
  - `POST /api/v1/sales/dispatch/confirm` → `SalesService.confirmDispatch(...)`
  - `POST /api/v1/dispatch/confirm` → `DispatchController.confirmDispatch(...)` (must not create divergent state)
- Finished goods reservations are consistent and safe:
  - Slip state transitions are monotonic (reservation flow cannot overwrite `DISPATCHED/BACKORDER/CANCELLED`).
  - Backorder cancellation clears `InventoryReservation` state (no “double-release” inventory).
  - Dispatch preview reflects reserved quantities for the specific slip/order (no false shortages).
- COGS cost basis is non-zero and correct when inventory exists:
  - WAC includes on-hand stock (available + reserved) and cannot drop to `0` purely due to reservations.
- Purchase invoices produce correct Inventory/Input-GST/AP postings; purchase returns reverse correctly.
- Sales returns produce correct credit notes (revenue/tax reversal + AR credit) and COGS reversal + inventory restock.
- Dealer/Supplier payments & settlements correctly update outstanding amounts and ledgers.
- GST return matches postings to configured GST input/output accounts for the period.
- Accounting period rules (open/locked/closed) enforced for postings.
- Posting dates and “today” semantics are company-timezone correct in posting-critical paths (no server-TZ drift).
- Orchestrator/integration status values are compatible end-to-end (no deprecated automation breakage if enabled).
- Refresh tokens and revocations are production-safe (restart/multi-node safe; revoked refresh tokens stay invalid until expiry).
- Accounting standards are unified:
  - Canonical journal references are consistent across modules (`SalesOrderReference`, `JournalReferenceResolver`, `ReferenceNumberService`) and do not allow duplicate canonical mappings.
  - Posting entrypoints converge on `AccountingFacade` patterns (no “hidden” alternate posting logic in modules).
- Frontend help documentation exists and matches behavior (for website/help pages): concise accounting flow docs describing O2C (dispatch → invoice → journals → ledger), P2P (GRN → purchase → journals → settlements), GST/non-GST handling, returns, and reversals.
- Full suite passes: `cd erp-domain && mvn -B -ntp verify` (including JaCoCo gates).

## Non-goals
- No new API endpoints, UI work, or new workflow features.
- No schema redesigns unrelated to correctness/stability.
- No changes to business policy unless required to fix incorrect postings or broken invariants.

## Accounting Rules & Invariants (must always hold)

### General
- Currency rounding: 2 decimal places, HALF_UP (multiple `currency(...)` helpers exist; behavior must remain consistent).
- Double-entry balance: every posted journal entry must balance exactly (posting tolerance is effectively zero).

### GST vs Non-GST (Sales)
- Sales GST treatment is encoded on `SalesOrder`:
  - `gstTreatment`: `NONE`, `PER_ITEM`, `ORDER_TOTAL` (see `SalesService` enum `GstTreatment`).
  - `gstInclusive`: when true, unit prices are treated as tax-inclusive for computations.
- Dispatch is the source of truth for shipped quantity postings:
  - `SalesService.confirmDispatch(...)` computes taxable base + GST + discounts per shipped quantity via `computeDispatchLineAmounts(...)`.
  - Non-GST is represented by `gstTreatment=NONE` and/or `taxRate=0`; tax totals must be 0 and tax lines must be empty.

### GST vs Non-GST (Purchases)
- Purchase journal posting rule (see `AccountingFacade.postPurchaseJournal(...)`):
  - Dr Inventory accounts (raw material inventory accounts)
  - Dr Input GST (company configured input tax account)
  - Cr Supplier Payable
  - Must satisfy `inventoryTotal + taxTotal == totalAmount` (exact).
- Purchase return reverses the above (see `AccountingFacade.postPurchaseReturn(...)`).

### Sales Posting Identity (Dispatch)
- AR/Revenue/Tax posting must satisfy:
  - Dr AR = total invoice amount
  - Cr Revenue + Cr Output GST − Dr Discounts = total invoice amount
  - No duplicate posting for the same dispatch/order (reference/idempotency must prevent re-posting).
- COGS posting must satisfy:
  - Dr COGS = Σ(shippedQty × unitCost)
  - Cr Inventory valuation accounts = same total
  - Inventory movements should be linked to the COGS journal (`InventoryMovement.journalEntryId`) when possible.

### Idempotency & State Transitions
- `SalesOrder` idempotency:
  - `idempotency_key` + `idempotency_hash` in `SalesService.createOrder(...)`.
  - posting markers: `salesJournalEntryId`, `cogsJournalEntryId`, `fulfillmentInvoiceId`.
- Dispatch idempotency:
  - `PackagingSlip.status == DISPATCHED` indicates inventory already issued.
  - `SalesService.confirmDispatch(...)` must be safe to retry and should only fill missing artifacts on a partially completed dispatch.
- Invoice payment idempotency:
  - `Invoice.paymentReferences` guards `InvoiceSettlementPolicy.applyPayment(...)`.

### Period Controls
- Posting date must map to an OPEN accounting period (`AccountingPeriodService.requireOpenPeriod(...)`).

### Finished Goods Reservations & Costing (critical invariants)
- Reservation invariants:
  - Batch “on-hand” must remain consistent: `quantityTotal >= quantityAvailable >= 0` (within tolerance).
  - Reservation flows must not mutate terminal slips (`DISPATCHED`, `CANCELLED`) or re-purpose `BACKORDER` slips.
  - Cancelling a backorder must cancel/release matching `InventoryReservation` rows (not just adjust totals).
- Costing invariants:
  - WAC must be based on on-hand inventory, not only unreserved availability.
  - Dispatch must not silently post `0` unit cost when inventory exists (fail closed with a clear error).

### Integration / Orchestrator Status Compatibility
- Any orchestrator-driven fulfillment update must use a status accepted by `IntegrationCoordinator.updateFulfillment(...)`, or be mapped safely.

### Session Security Invariants
- Refresh tokens must be durable (restart/multi-node safe) and revocation must persist for the refresh token lifetime (or refresh tokens are explicitly revoked).

## Modules & boundaries touched (paths)
- Sales dispatch + GST:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesJournalService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/SalesController.java`
- Inventory dispatch:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/DispatchController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGoodBatchRepository.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/*` (PackagingSlip, InventoryMovement, InventoryReservation, …)
- Orchestrator / integration:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/controller/OrchestratorController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java`
- Auth / security:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/RefreshTokenService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/TokenBlacklistService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/GlobalExceptionHandler.java`
- Invoices + settlements:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceSettlementPolicy.java`
- Accounting core:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingFacade.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyAccountingSettingsService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/TaxService.java`
- Purchasing:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchasingService.java`
- Returns:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java`
- Reporting / reconciliation:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/service/ReportService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java`
- Security / tenancy:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextFilter.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextHolder.java`

## Cross-module linkage expectations
- Dispatch confirmation:
  - APIs:
    - `POST /api/v1/sales/dispatch/confirm` → `SalesController.confirmDispatch(...)` → `SalesService.confirmDispatch(...)`
    - `POST /api/v1/dispatch/confirm` → `DispatchController.confirmDispatch(...)` → `SalesService.confirmDispatch(...)` (must be consistent; avoid duplicate/partial artifacts)
  - Flow:
    - `SalesService.confirmDispatch(...)` → `FinishedGoodsService.confirmDispatch(...)` (inventory issue)
    - `SalesService.confirmDispatch(...)` → `AccountingFacade.postCogsJournal(...)` (COGS)
    - `SalesService.confirmDispatch(...)` → `AccountingFacade.postSalesJournal(...)` (AR/Rev/Tax)
    - `SalesService.confirmDispatch(...)` → `InvoiceRepository.save(...)` + `DealerLedgerService.syncInvoiceLedger(...)`
    - `SalesService.confirmDispatch(...)` updates `PackagingSlip.(journal_entry_id, cogs_journal_entry_id, invoice_id)` and `SalesOrder` posting markers.
- Purchase invoice:
  - `PurchasingService` → `AccountingFacade.postPurchaseJournal(...)` → `AccountingService.createJournalEntry(...)`.
  - `RawMaterialService.recordReceipt(...)` records raw material movements and links journals when posting is enabled.
- Production:
  - `ProductionLogService.postMaterialJournal(...)` → `AccountingFacade.postMaterialConsumption(...)` → link `RawMaterialMovement.journalEntryId`.
  - `PackingService.postPackingSessionJournal(...)` posts WIP→FG and links `InventoryMovement.journalEntryId`.
- Sales returns:
  - `SalesReturnService.processReturn(...)` → `AccountingFacade.postSalesReturn(...)` (+ COGS reversal via `AccountingFacade.postInventoryAdjustment(...)`) and restock movements.

## Risk register (top risks + mitigations)
1. Duplicate/incorrect postings due to mixed reference schemes (invoice number vs canonical order reference)
   - Mitigation: unify reference strategy or add explicit mapping; add idempotency tests around dispatch/invoice issuance.
2. Rounding drift causing unbalanced journals (zero tolerance posting)
   - Mitigation: centralize rounding for money/tax; add invariant tests that assert exact balances after rounding.
3. Multi-tenant authorization gaps (`CompanyContextFilter` “allow” paths)
   - Mitigation: audit all unauthenticated endpoints and ensure they don’t call company-scoped services; add security tests for cross-company access.
4. Duplicate cross-module entry points for dispatch confirmation (two endpoints + multiple inventory dispatch methods)
   - Mitigation: pick a single authoritative flow (`SalesService.confirmDispatch(...)`) and ensure all controllers route to it; add tests to ensure both endpoints remain equivalent and idempotent.
5. Event-based inventory accounting listener is currently detached (risk of future double-posting if wired later)
   - Mitigation: either wire with strong idempotency and remove manual postings, or keep disabled and document; add a guard test that prevents double posting when both paths exist.
6. `journal_reference_mappings` canonical reference is not unique (Optional repository access can explode if duplicates exist)
   - Mitigation: add unique constraint or adjust repository to tolerate multi-row; add migration + regression test.
7. Large API surface (`openapi.json` ~200+ paths) increases drift risk (duplicate endpoints, inconsistent auth/validation)
   - Mitigation: inventory endpoints by module, deprecate/merge duplicates only when safe, add regression coverage for “critical path” endpoints, and document supported flows.
8. Inventory reservation drift can understate COGS / overstate availability
   - Mitigation: fix WAC to include on-hand; enforce slip transition rules; reconcile reservation rows on backorder cancel; add targeted tests.
9. Refresh tokens + revocations are not production-safe (restart/multi-node + retention mismatch)
   - Mitigation: persist refresh tokens and align revocation retention with refresh TTL; add tests that simulate restart and long-lived refresh.

## Acceptance criteria (checklist)
- [ ] `SalesService.confirmDispatch(...)` is idempotent: re-POST does not change inventory or create new journals/invoices.
- [ ] Both dispatch endpoints behave equivalently and are safe to retry: `/api/v1/sales/dispatch/confirm` and `/api/v1/dispatch/confirm`.
- [ ] WAC uses on-hand (reserved + available) and dispatch cost cannot become `0` when inventory exists.
- [ ] Reservation flow never overwrites terminal slip statuses (`DISPATCHED/BACKORDER/CANCELLED`).
- [ ] Cancel backorder clears `InventoryReservation` rows and reconciles batch availability correctly.
- [ ] Dispatch preview reflects reserved quantities for the specific slip/order (no false shortages).
- [ ] Orchestrator fulfillment status values are compatible end-to-end (or safely mapped).
- [ ] Posting dates use company timezone in posting-critical paths (no server-TZ drift).
- [ ] Refresh tokens survive restart/multi-node and revoked refresh tokens remain invalid until expiry.
- [ ] `TaxService.generateGstReturn(...)` output tax equals sum of posted output GST lines for the period; input tax similarly.
- [ ] Purchase invoice and return postings balance and use configured GST input account.
- [ ] Sales return posts a credit note + COGS reversal and restocks inventory; AR reduced and dealer ledger updated.
- [ ] Trial balance is balanced (see `ReportService.trialBalance()`), and invariant suites pass.
- [ ] Documentation exists for frontend/help pages and matches behavior (no contradictions with code/tests).
- [ ] Full suite passes: `cd erp-domain && mvn -B -ntp verify`.
