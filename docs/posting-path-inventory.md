# Posting Path Inventory (Task 00 / EPIC 05 — Milestone 01)

This inventory lists every journal-posting call path and highlights divergences from the preferred `AccountingFacade` flow.

## How this inventory was built
- `rg -n "createJournalEntry\\(" erp-domain/src/main/java -g "*.java"`
- `rg -n "accountingFacade\\." erp-domain/src/main/java -g "*.java"`

## Preferred paths (via `AccountingFacade`)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/InventoryAdjustmentService.java` → `accountingFacade.postInventoryAdjustment(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/RawMaterialService.java` → `accountingFacade.postPurchaseJournal(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesJournalService.java` → `accountingFacade.postSalesJournal(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesFulfillmentService.java` → `accountingFacade.postCogsJournal(...)` (and `hasCogsJournalFor(...)`)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java` → `accountingFacade.postCogsJournal(...)`, `accountingFacade.postSalesJournal(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java` → `accountingFacade.postSalesReturn(...)`, `accountingFacade.postInventoryAdjustment(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/CostAllocationService.java` → `accountingFacade.postCostVarianceAllocation(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchasingService.java` → `accountingFacade.postPurchaseReturn(...)`, `accountingFacade.postPurchaseJournal(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/ProductionLogService.java` → `accountingFacade.postSimpleJournal(...)`, `accountingFacade.postMaterialConsumption(...)`, `accountingFacade.postLaborOverheadApplied(...)`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/PackingService.java` → `accountingFacade.postSimpleJournal(...)`

## Direct `AccountingService.createJournalEntry(...)` paths (review + decision)
| Call site | Purpose | Decision |
| --- | --- | --- |
| `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollService.java` | Payroll posting | Keep direct (no facade equivalent; payroll-specific lines). |
| `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/OpeningStockImportService.java` | Opening stock import | Keep direct (one-off opening balance flow). |
| `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/PackingService.java` | FG receipt posting | Migrate to facade (already uses `postSimpleJournal` elsewhere). |
| `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/BulkPackingService.java` | Bulk pack posting | Review (no facade equivalent; consider adding a facade method). |
| `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/InventoryAccountingEventListener.java` | Event-driven postings | Keep disabled unless wired with idempotency guard. |
| `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingController.java` | Manual journal entry API | Keep direct (intentionally manual). |
| `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/MockDataInitializer.java` | Dev/test seeding | Keep direct (non-prod only). |

## Divergence notes (candidates for consolidation)
- `PackingService` is the top consolidation target (dual-path usage).
- `BulkPackingService` and `PayrollService` lack facade equivalents today; any migration should preserve existing reference/idempotency behavior.

## Journal-less accounting side effects (quick scan)
- No obvious journal-less posting paths found in `rg -n "createJournalEntry\\("` and `rg -n "accountingFacade\\."` scan.
- Ledger syncs (`DealerLedgerService`, `SupplierLedgerService`) remain coupled to journal/settlement flows; verify again if new posting paths are introduced.
