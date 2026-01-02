# ERP Test Matrix

## Record-to-Report (Accounting Core)
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/JournalEntryE2ETest.java`: manual journals + validations
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/PeriodCloseLockIT.java`: period close/lock + reopening
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/CriticalAccountingAxesIT.java`: journal invariants + reversals
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/StatementAgingIT.java`: statements + aging

## Order-to-Cash (Sales + AR)
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/sales/OrderFulfillmentE2ETest.java`: order lifecycle + reservations
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/sales/DealerLedgerIT.java`: dealer ledger + balances
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/inventory/DispatchConfirmationIT.java`: dispatch confirmation + inventory
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/sales/GstInclusiveRoundingIT.java`: GST rounding behavior
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/fullcycle/FullCycleE2ETest.java`: end-to-end order-to-cash (disabled)

## Procure-to-Pay (Purchasing + AP)
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchasingServiceTest.java`: purchase validation + journal posting
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/SettlementE2ETest.java`: supplier/dealer settlements + allocations
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/LandedCostRevaluationIT.java`: purchase cost adjustments

## Produce-to-Stock (Factory/Production + Inventory)
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/production/CompleteProductionCycleTest.java`: production log to packing
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/production/FactoryPackagingCostingIT.java`: packaging costing
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/WipToFinishedCostIT.java`: WIP to finished goods costing

## Hire-to-Pay (HR/Payroll)
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/PayrollBatchPaymentIT.java`: payroll batch payments
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/hr/HrControllerIT.java`: employee + attendance flows

## Cross-cutting invariants and reconciliation
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/inventory/InventoryGlReconciliationIT.java`: inventory vs GL
- `erp-domain/src/test/java/com/bigbrightpaints/erp/e2e/accounting/CriticalAccountingAxesIT.java`: double-entry + reversals
- `erp-domain/src/test/java/com/bigbrightpaints/erp/smoke/CriticalPathSmokeTest.java`: core endpoints availability
- `erp-domain/src/test/java/com/bigbrightpaints/erp/invariants/ErpInvariantsSuiteIT.java`: golden paths + invariant checks
