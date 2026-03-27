# Entry Point Map

This document maps all entry points in BigBrightPaints ERP: REST endpoints, event consumers/listeners, scheduled jobs, filters, and interceptors.

---

## REST Endpoints by Module

### Orchestrator Module

| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/orchestrator/orders/{orderId}/approve` | `approveOrder` | Admin |
| POST | `/api/v1/orchestrator/orders/{orderId}/fulfillment` | `fulfillOrder` | Admin, Factory |
| POST | `/api/v1/orchestrator/factory/dispatch/{batchId}` | `dispatchBatch` | Admin, Factory |
| POST | `/api/v1/orchestrator/dispatch` | `dispatchOrder` | Admin, Factory |
| POST | `/api/v1/orchestrator/dispatch/{orderId}` | `dispatchOrderById` | Admin, Factory |
| POST | `/api/v1/orchestrator/payroll/run` | `runPayroll` | Admin, HR |
| GET | `/api/v1/orchestrator/traces/{traceId}` | `getTrace` | Admin |
| GET | `/api/v1/orchestrator/health/integrations` | `getIntegrationHealth` | Admin |
| GET | `/api/v1/orchestrator/health/events` | `getEventHealth` | Admin |

#### Dashboard Endpoints
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/orchestrator/dashboard/admin` | `getAdminDashboard` | Admin |
| GET | `/api/v1/orchestrator/dashboard/factory` | `getFactoryDashboard` | Admin, Factory |
| GET | `/api/v1/orchestrator/dashboard/finance` | `getFinanceDashboard` | Admin, Accounting |

---

### Accounting Module

#### Accounts & Configuration
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/accounting/accounts` | `listAccounts` | Admin, Accounting |
| POST | `/api/v1/accounting/accounts` | `createAccount` | Admin, Accounting |
| GET | `/api/v1/accounting/default-accounts` | `getDefaultAccounts` | Admin, Accounting |
| PUT | `/api/v1/accounting/default-accounts` | `updateDefaultAccounts` | Admin, Accounting |
| GET | `/api/v1/accounting/configuration/health` | `getConfigurationHealth` | Admin, Accounting |

#### Journal Entries
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/accounting/journal-entries` | `listJournalEntries` | Admin, Accounting |
| POST | `/api/v1/accounting/journal-entries` | `createJournalEntry` | Admin, Accounting |
| POST | `/api/v1/accounting/journal-entries/{entryId}/reverse` | `reverseJournalEntry` | Admin, Accounting |
| POST | `/api/v1/accounting/journal-entries/{entryId}/cascade-reverse` | `cascadeReverseEntry` | Admin, Accounting |
| GET | `/api/v1/accounting/journals` | `listJournals` | Admin, Accounting |
| POST | `/api/v1/accounting/journals/manual` | `createManualJournal` | Admin, Accounting |
| POST | `/api/v1/accounting/journals/{entryId}/reverse` | `reverseJournal` | Admin, Accounting |

#### Period Management
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/accounting/periods` | `listPeriods` | Admin, Accounting |
| POST | `/api/v1/accounting/periods` | `createPeriod` | Admin, Accounting |
| PUT | `/api/v1/accounting/periods/{id}` | `updatePeriod` | Admin, Accounting |
| POST | `/api/v1/accounting/periods/{id}/close` | `closePeriod` | Admin, Accounting |
| POST | `/api/v1/accounting/periods/{id}/reopen` | `reopenPeriod` | Admin, Accounting |
| GET | `/api/v1/accounting/periods/{id}/checklist` | `getCloseChecklist` | Admin, Accounting |
| PUT | `/api/v1/accounting/periods/{id}/checklist` | `updateChecklist` | Admin, Accounting |

#### Dealer Receipts & Settlements
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/accounting/receipts/dealer` | `recordDealerReceipt` | Admin, Accounting |
| POST | `/api/v1/accounting/receipts/dealer/hybrid` | `recordDealerReceiptSplit` | Admin, Accounting |
| POST | `/api/v1/accounting/settlements/dealers` | `settleDealerInvoices` | Admin, Accounting |
| POST | `/api/v1/accounting/dealers/{dealerId}/auto-settle` | `autoSettleDealer` | Admin, Accounting |

#### Supplier Payments & Settlements
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/accounting/suppliers/payments` | `recordSupplierPayment` | Admin, Accounting |
| POST | `/api/v1/accounting/settlements/suppliers` | `settleSupplierInvoices` | Admin, Accounting |
| POST | `/api/v1/accounting/suppliers/{supplierId}/auto-settle` | `autoSettleSupplier` | Admin, Accounting |

#### Credit/Debit Notes & Adjustments
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/accounting/credit-notes` | `postCreditNote` | Admin, Accounting |
| POST | `/api/v1/accounting/debit-notes` | `postDebitNote` | Admin, Accounting |
| POST | `/api/v1/accounting/accruals` | `postAccrual` | Admin, Accounting |
| POST | `/api/v1/accounting/bad-debts/write-off` | `writeOffBadDebt` | Admin, Accounting |

#### Payroll Payments
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/accounting/payroll/payments/batch` | `processPayrollBatchPayment` | Admin, Accounting |

#### GST & Tax
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/accounting/gst/return` | `generateGstReturn` | Admin, Accounting |
| GET | `/api/v1/accounting/gst/reconciliation` | `getGstReconciliation` | Admin, Accounting |

#### Sales Returns
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/accounting/sales/returns` | `listSalesReturns` | Admin, Accounting, Sales |
| POST | `/api/v1/accounting/sales/returns/preview` | `previewSalesReturn` | Admin, Accounting, Sales |
| POST | `/api/v1/accounting/sales/returns` | `postSalesReturn` | Admin, Accounting, Sales |

#### Audit Trail
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/accounting/audit-trail` | `getAuditTrail` | Admin, Accounting |

#### Bank Reconciliation
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/accounting/reconciliation/sessions` | `createReconciliationSession` | Admin, Accounting |
| GET | `/api/v1/accounting/reconciliation/sessions/{id}` | `getReconciliationSession` | Admin, Accounting |
| PUT | `/api/v1/accounting/reconciliation/sessions/{id}/items` | `updateSessionItems` | Admin, Accounting |
| POST | `/api/v1/accounting/reconciliation/sessions/{id}/complete` | `completeReconciliation` | Admin, Accounting |
| GET | `/api/v1/accounting/reconciliation/discrepancies` | `listDiscrepancies` | Admin, Accounting |
| POST | `/api/v1/accounting/reconciliation/discrepancies/{id}/resolve` | `resolveDiscrepancy` | Admin, Accounting |

#### Opening Balances & Imports
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/accounting/opening-balances` | `importOpeningBalances` | Admin, Accounting |
| POST | `/api/v1/migration/tally-import` | `importFromTally` | Admin, Accounting |

---

### Sales Module

#### Dealers
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/dealers` | `listDealers` | Admin, Sales, Accounting |
| POST | `/api/v1/dealers` | `createDealer` | Admin, Sales |
| PUT | `/api/v1/dealers/{dealerId}` | `updateDealer` | Admin, Sales |
| GET | `/api/v1/dealers/search` | `searchDealers` | Admin, Sales, Accounting |
| GET | `/api/v1/dealers/{dealerId}/ledger` | `getDealerLedger` | Admin, Sales, Accounting |
| GET | `/api/v1/dealers/{dealerId}/invoices` | `getDealerInvoices` | Admin, Sales, Accounting |
| GET | `/api/v1/dealers/{dealerId}/credit-utilization` | `getCreditUtilization` | Admin, Sales, Accounting |
| GET | `/api/v1/dealers/{dealerId}/aging` | `getDealerAging` | Admin, Sales, Accounting |
| POST | `/api/v1/dealers/{dealerId}/dunning/hold` | `holdForDunning` | Admin, Sales, Accounting |

#### Sales Orders
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/sales/orders` | `listOrders` | Admin, Sales, Factory, Accounting |
| GET | `/api/v1/sales/orders/search` | `searchOrders` | Admin, Sales, Factory, Accounting |
| POST | `/api/v1/sales/orders` | `createOrder` | Admin, Sales |
| PUT | `/api/v1/sales/orders/{id}` | `updateOrder` | Admin, Sales |
| DELETE | `/api/v1/sales/orders/{id}` | `deleteOrder` | Admin, Sales |
| POST | `/api/v1/sales/orders/{id}/confirm` | `confirmOrder` | Admin, Sales |
| POST | `/api/v1/sales/orders/{id}/cancel` | `cancelOrder` | Admin, Sales |
| PATCH | `/api/v1/sales/orders/{id}/status` | `updateOrderStatus` | Admin, Sales |
| GET | `/api/v1/sales/orders/{id}/timeline` | `getOrderTimeline` | Admin, Sales |

#### Sales Dealers Alias
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/sales/dealers` | `listDealersAlias` | Admin, Sales, Accounting |
| GET | `/api/v1/sales/dealers/search` | `searchDealersAlias` | Admin, Sales, Accounting |

#### Dashboard
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/sales/dashboard` | `getSalesDashboard` | Admin, Sales |

#### Promotions
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/sales/promotions` | `listPromotions` | Admin, Sales |
| POST | `/api/v1/sales/promotions` | `createPromotion` | Admin, Sales |
| PUT | `/api/v1/sales/promotions/{id}` | `updatePromotion` | Admin, Sales |
| DELETE | `/api/v1/sales/promotions/{id}` | `deletePromotion` | Admin, Sales |

#### Sales Targets
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/sales/targets` | `listTargets` | Admin, Sales |
| POST | `/api/v1/sales/targets` | `createTarget` | Admin, Sales |
| PUT | `/api/v1/sales/targets/{id}` | `updateTarget` | Admin, Sales |
| DELETE | `/api/v1/sales/targets/{id}` | `deleteTarget` | Admin, Sales |

#### Dispatch Reconciliation
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/sales/dispatch/reconcile-order-markers` | `reconcileOrderMarkers` | Admin, Sales, Factory |

#### Credit Limit Override Requests
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/credit/override-requests` | `listOverrideRequests` | Admin, Accounting |
| POST | `/api/v1/credit/override-requests` | `createOverrideRequest` | Admin, Sales |
| POST | `/api/v1/credit/override-requests/{id}/approve` | `approveOverride` | Admin, Accounting |
| POST | `/api/v1/credit/override-requests/{id}/reject` | `rejectOverride` | Admin, Accounting |

#### Credit Limit Requests
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/credit/limit-requests` | `listLimitRequests` | Admin, Accounting |
| POST | `/api/v1/credit/limit-requests` | `createLimitRequest` | Admin, Sales |
| POST | `/api/v1/credit/limit-requests/{id}/approve` | `approveLimit` | Admin, Accounting |
| POST | `/api/v1/credit/limit-requests/{id}/reject` | `rejectLimit` | Admin, Accounting |

---

### Dealer Portal Module

| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/dealer-portal/dashboard` | `getDealerDashboard` | Dealer |
| GET | `/api/v1/dealer-portal/ledger` | `getDealerLedger` | Dealer |
| GET | `/api/v1/dealer-portal/invoices` | `getDealerInvoices` | Dealer |
| GET | `/api/v1/dealer-portal/aging` | `getDealerAging` | Dealer |
| GET | `/api/v1/dealer-portal/orders` | `getDealerOrders` | Dealer |
| POST | `/api/v1/dealer-portal/credit-limit-requests` | `createCreditLimitRequest` | Dealer |
| GET | `/api/v1/dealer-portal/invoices/{invoiceId}/pdf` | `downloadInvoicePdf` | Dealer |

---

### Purchasing Module

#### Suppliers
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/suppliers` | `listSuppliers` | Admin, Purchasing, Accounting |
| GET | `/api/v1/suppliers/{id}` | `getSupplier` | Admin, Purchasing, Accounting |
| POST | `/api/v1/suppliers` | `createSupplier` | Admin, Purchasing |
| PUT | `/api/v1/suppliers/{id}` | `updateSupplier` | Admin, Purchasing |
| POST | `/api/v1/suppliers/{id}/approve` | `approveSupplier` | Admin, Purchasing |
| POST | `/api/v1/suppliers/{id}/activate` | `activateSupplier` | Admin, Purchasing |
| POST | `/api/v1/suppliers/{id}/suspend` | `suspendSupplier` | Admin, Purchasing |

#### Purchase Orders
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/purchasing/purchase-orders` | `listPurchaseOrders` | Admin, Purchasing, Accounting |
| GET | `/api/v1/purchasing/purchase-orders/{id}` | `getPurchaseOrder` | Admin, Purchasing, Accounting |
| POST | `/api/v1/purchasing/purchase-orders` | `createPurchaseOrder` | Admin, Purchasing |
| POST | `/api/v1/purchasing/purchase-orders/{id}/approve` | `approvePurchaseOrder` | Admin, Purchasing |
| POST | `/api/v1/purchasing/purchase-orders/{id}/void` | `voidPurchaseOrder` | Admin, Purchasing |
| POST | `/api/v1/purchasing/purchase-orders/{id}/close` | `closePurchaseOrder` | Admin, Purchasing |
| GET | `/api/v1/purchasing/purchase-orders/{id}/timeline` | `getPOTimeline` | Admin, Purchasing |

#### Goods Receipts
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/purchasing/goods-receipts` | `listGoodsReceipts` | Admin, Purchasing, Accounting |
| GET | `/api/v1/purchasing/goods-receipts/{id}` | `getGoodsReceipt` | Admin, Purchasing, Accounting |
| POST | `/api/v1/purchasing/goods-receipts` | `createGoodsReceipt` | Admin, Purchasing |

#### Raw Material Purchases
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/purchasing/raw-material-purchases` | `listRawMaterialPurchases` | Admin, Purchasing, Accounting |
| GET | `/api/v1/purchasing/raw-material-purchases/{id}` | `getRawMaterialPurchase` | Admin, Purchasing, Accounting |
| POST | `/api/v1/purchasing/raw-material-purchases` | `createRawMaterialPurchase` | Admin, Purchasing |
| POST | `/api/v1/purchasing/raw-material-purchases/returns` | `createPurchaseReturn` | Admin, Purchasing |
| POST | `/api/v1/purchasing/raw-material-purchases/returns/preview` | `previewPurchaseReturn` | Admin, Purchasing |

---

### Inventory Module

#### Finished Goods
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/finished-goods` | `listFinishedGoods` | Admin, Inventory, Factory, Sales |
| GET | `/api/v1/finished-goods/{id}` | `getFinishedGood` | Admin, Inventory, Factory |
| GET | `/api/v1/finished-goods/{id}/batches` | `getBatchesForGood` | Admin, Inventory, Factory |
| POST | `/api/v1/finished-goods/{id}/batches` | `createBatch` | Admin, Inventory, Factory |
| GET | `/api/v1/finished-goods/stock-summary` | `getStockSummary` | Admin, Inventory, Factory |
| GET | `/api/v1/finished-goods/low-stock` | `getLowStock` | Admin, Inventory, Factory |
| GET | `/api/v1/finished-goods/{id}/low-stock-threshold` | `getLowStockThreshold` | Admin, Inventory |
| PUT | `/api/v1/finished-goods/{id}/low-stock-threshold` | `setLowStockThreshold` | Admin, Inventory |

#### Raw Materials
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/raw-materials/stock` | `listRawMaterialStock` | Admin, Inventory, Factory |
| GET | `/api/v1/raw-materials/stock/inventory` | `getRawMaterialInventory` | Admin, Inventory, Factory |
| GET | `/api/v1/raw-materials/stock/low-stock` | `getLowStockRawMaterials` | Admin, Inventory, Factory |
| POST | `/api/v1/inventory/raw-materials/adjustments` | `createRawMaterialAdjustment` | Admin, Inventory |

#### Inventory Adjustments
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/inventory/adjustments` | `listAdjustments` | Admin, Inventory, Accounting |
| POST | `/api/v1/inventory/adjustments` | `createAdjustment` | Admin, Inventory |

#### Inventory Batches
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/inventory/batches/{id}/movements` | `getBatchMovements` | Admin, Inventory |

#### Opening Stock
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/inventory/opening-stock` | `importOpeningStock` | Admin, Inventory |
| GET | `/api/v1/inventory/opening-stock` | `listOpeningStockImports` | Admin, Inventory |

#### Dispatch
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/dispatch/pending` | `listPendingDispatches` | Admin, Inventory, Factory |
| GET | `/api/v1/dispatch/preview/{slipId}` | `previewDispatch` | Admin, Inventory, Factory |
| POST | `/api/v1/dispatch/backorder/{slipId}/cancel` | `cancelBackorder` | Admin, Inventory, Factory |
| GET | `/api/v1/dispatch/slip/{slipId}` | `getPackagingSlip` | Admin, Inventory, Factory |
| GET | `/api/v1/dispatch/order/{orderId}` | `getDispatchForOrder` | Admin, Inventory, Factory, Sales |
| POST | `/api/v1/dispatch/confirm` | `confirmDispatch` | Admin, Inventory, Factory |
| GET | `/api/v1/dispatch/slip/{slipId}/challan/pdf` | `downloadDeliveryChallan` | Admin, Inventory |
| PATCH | `/api/v1/dispatch/slip/{slipId}/status` | `updateSlipStatus` | Admin, Inventory |

---

### Factory Module

#### Production Plans
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/factory/production-plans` | `listProductionPlans` | Admin, Factory |
| POST | `/api/v1/factory/production-plans` | `createProductionPlan` | Admin, Factory |
| PUT | `/api/v1/factory/production-plans/{id}` | `updateProductionPlan` | Admin, Factory |
| PATCH | `/api/v1/factory/production-plans/{id}/status` | `updatePlanStatus` | Admin, Factory |
| DELETE | `/api/v1/factory/production-plans/{id}` | `deleteProductionPlan` | Admin, Factory |

#### Factory Tasks
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/factory/tasks` | `listFactoryTasks` | Admin, Factory |
| POST | `/api/v1/factory/tasks` | `createFactoryTask` | Admin, Factory |
| PUT | `/api/v1/factory/tasks/{id}` | `updateFactoryTask` | Admin, Factory |

#### Dashboard & Costing
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/factory/dashboard` | `getFactoryDashboard` | Admin, Factory |
| POST | `/api/v1/factory/cost-allocation` | `allocateCosts` | Admin, Factory, Accounting |

#### Packing Records
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/factory/packing-records` | `createPackingRecord` | Admin, Factory |
| GET | `/api/v1/factory/unpacked-batches` | `listUnpackedBatches` | Admin, Factory |
| GET | `/api/v1/factory/production-logs/{productionLogId}/packing-history` | `getPackingHistory` | Admin, Factory |
| GET | `/api/v1/factory/bulk-batches/{finishedGoodId}` | `getBulkBatches` | Admin, Factory |
| GET | `/api/v1/factory/bulk-batches/{parentBatchId}/children` | `getChildBatches` | Admin, Factory |

#### Production Logs
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/factory/production/logs` | `createProductionLog` | Admin, Factory |
| GET | `/api/v1/factory/production/logs` | `listProductionLogs` | Admin, Factory |
| GET | `/api/v1/factory/production/logs/{id}` | `getProductionLog` | Admin, Factory |

#### Packaging Mappings
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/factory/packaging-mappings` | `listPackagingMappings` | Admin, Factory |
| GET | `/api/v1/factory/packaging-mappings/active` | `listActiveMappings` | Admin, Factory |
| POST | `/api/v1/factory/packaging-mappings` | `createPackagingMapping` | Admin, Factory |
| PUT | `/api/v1/factory/packaging-mappings/{id}` | `updatePackagingMapping` | Admin, Factory |
| DELETE | `/api/v1/factory/packaging-mappings/{id}` | `deletePackagingMapping` | Admin, Factory |

---

### Production/Catalog Module

#### Brands
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/catalog/brands` | `createBrand` | Admin, Production |
| GET | `/api/v1/catalog/brands` | `listBrands` | Admin, Production, Sales |
| GET | `/api/v1/catalog/brands/{brandId}` | `getBrand` | Admin, Production |
| PUT | `/api/v1/catalog/brands/{brandId}` | `updateBrand` | Admin, Production |
| DELETE | `/api/v1/catalog/brands/{brandId}` | `deleteBrand` | Admin, Production |

#### Catalog Import
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/catalog/import` | `importCatalog` | Admin, Production |

#### Catalog Items
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/catalog/items` | `createCatalogItem` | Admin, Production |
| GET | `/api/v1/catalog/items` | `listCatalogItems` | Admin, Production, Sales, Factory |
| GET | `/api/v1/catalog/items/{itemId}` | `getCatalogItem` | Admin, Production |
| PUT | `/api/v1/catalog/items/{itemId}` | `updateCatalogItem` | Admin, Production |
| DELETE | `/api/v1/catalog/items/{itemId}` | `deleteCatalogItem` | Admin, Production |

---

### HR Module

#### Employees
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/hr/employees` | `listEmployees` | Admin, HR |
| POST | `/api/v1/hr/employees` | `createEmployee` | Admin, HR |
| PUT | `/api/v1/hr/employees/{id}` | `updateEmployee` | Admin, HR |
| DELETE | `/api/v1/hr/employees/{id}` | `deleteEmployee` | Admin, HR |

#### Salary Structures
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/hr/salary-structures` | `listSalaryStructures` | Admin, HR |
| POST | `/api/v1/hr/salary-structures` | `createSalaryStructure` | Admin, HR |
| PUT | `/api/v1/hr/salary-structures/{id}` | `updateSalaryStructure` | Admin, HR |

#### Leave Management
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/hr/leave-requests` | `listLeaveRequests` | Admin, HR |
| POST | `/api/v1/hr/leave-requests` | `createLeaveRequest` | Admin, HR |
| PATCH | `/api/v1/hr/leave-requests/{id}/status` | `updateLeaveStatus` | Admin, HR |
| GET | `/api/v1/hr/leave-types` | `listLeaveTypes` | Admin, HR |
| GET | `/api/v1/hr/employees/{employeeId}/leave-balances` | `getLeaveBalances` | Admin, HR |

#### Attendance
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/hr/attendance/date/{date}` | `getAttendanceByDate` | Admin, HR |
| GET | `/api/v1/hr/attendance/today` | `getTodayAttendance` | Admin, HR |
| GET | `/api/v1/hr/attendance/summary` | `getAttendanceSummary` | Admin, HR |
| GET | `/api/v1/hr/attendance/summary/monthly` | `getMonthlyAttendanceSummary` | Admin, HR |
| GET | `/api/v1/hr/attendance/employee/{employeeId}` | `getEmployeeAttendance` | Admin, HR |
| POST | `/api/v1/hr/attendance/mark/{employeeId}` | `markAttendance` | Admin, HR |
| POST | `/api/v1/hr/attendance/bulk-mark` | `bulkMarkAttendance` | Admin, HR |
| POST | `/api/v1/hr/attendance/bulk-import` | `bulkImportAttendance` | Admin, HR |

#### Payroll Runs (HR Controller)
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/hr/payroll-runs` | `listPayrollRuns` | Admin, HR |
| POST | `/api/v1/hr/payroll-runs` | `createPayrollRun` | Admin, HR |

#### Payroll Runs (Payroll Controller)
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/payroll/runs` | `listPayrollRuns` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/runs/weekly` | `listWeeklyRuns` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/runs/monthly` | `listMonthlyRuns` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/runs/{id}` | `getPayrollRun` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/runs/{id}/lines` | `getPayrollRunLines` | Admin, HR, Accounting |
| POST | `/api/v1/payroll/runs` | `createPayrollRun` | Admin, HR |
| POST | `/api/v1/payroll/runs/weekly` | `createWeeklyRun` | Admin, HR |
| POST | `/api/v1/payroll/runs/monthly` | `createMonthlyRun` | Admin, HR |
| POST | `/api/v1/payroll/runs/{id}/calculate` | `calculatePayroll` | Admin, HR |
| POST | `/api/v1/payroll/runs/{id}/approve` | `approvePayroll` | Admin, HR |
| POST | `/api/v1/payroll/runs/{id}/post` | `postPayroll` | Admin, HR, Accounting |
| POST | `/api/v1/payroll/runs/{id}/mark-paid` | `markPayrollPaid` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/summary/weekly` | `getWeeklySummary` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/summary/monthly` | `getMonthlySummary` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/summary/current-week` | `getCurrentWeekSummary` | Admin, HR, Accounting |
| GET | `/api/v1/payroll/summary/current-month` | `getCurrentMonthSummary` | Admin, HR, Accounting |

---

### Invoice Module

| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/invoices` | `listInvoices` | Admin, Sales, Accounting |
| GET | `/api/v1/invoices/{id}` | `getInvoice` | Admin, Sales, Accounting |
| GET | `/api/v1/invoices/{id}/pdf` | `downloadInvoicePdf` | Admin, Sales, Accounting |
| GET | `/api/v1/invoices/dealers/{dealerId}` | `getInvoicesByDealer` | Admin, Sales, Accounting |
| POST | `/api/v1/invoices/{id}/email` | `emailInvoice` | Admin, Sales, Accounting |

---

### Reports Module

| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/reports/balance-sheet` | `getBalanceSheet` | Admin, Accounting |
| GET | `/api/v1/reports/profit-loss` | `getProfitLoss` | Admin, Accounting |
| GET | `/api/v1/reports/cash-flow` | `getCashFlow` | Admin, Accounting |
| GET | `/api/v1/reports/inventory-valuation` | `getInventoryValuation` | Admin, Accounting, Inventory |
| GET | `/api/v1/reports/gst-return` | `getGstReturnReport` | Admin, Accounting |
| GET | `/api/v1/reports/inventory-reconciliation` | `getInventoryReconciliation` | Admin, Accounting, Inventory |
| GET | `/api/v1/reports/balance-warnings` | `getBalanceWarnings` | Admin, Accounting |
| GET | `/api/v1/reports/reconciliation-dashboard` | `getReconciliationDashboard` | Admin, Accounting |
| GET | `/api/v1/reports/trial-balance` | `getTrialBalance` | Admin, Accounting |
| GET | `/api/v1/reports/account-statement` | `getAccountStatement` | Admin, Accounting |
| GET | `/api/v1/reports/aged-debtors` | `getAgedDebtors` | Admin, Accounting |
| GET | `/api/v1/reports/balance-sheet/hierarchy` | `getBalanceSheetHierarchy` | Admin, Accounting |
| GET | `/api/v1/reports/income-statement/hierarchy` | `getIncomeStatementHierarchy` | Admin, Accounting |
| GET | `/api/v1/reports/aging/receivables` | `getReceivablesAging` | Admin, Accounting |
| GET | `/api/v1/reports/aging/dealer/{dealerId}` | `getDealerAgingReport` | Admin, Accounting, Sales |
| GET | `/api/v1/reports/aging/dealer/{dealerId}/detailed` | `getDetailedDealerAging` | Admin, Accounting, Sales |
| GET | `/api/v1/reports/dso/dealer/{dealerId}` | `getDealerDso` | Admin, Accounting, Sales |
| GET | `/api/v1/reports/wastage` | `getWastageReport` | Admin, Factory, Accounting |
| GET | `/api/v1/reports/production-logs/{id}/cost-breakdown` | `getCostBreakdown` | Admin, Factory, Accounting |
| GET | `/api/v1/reports/monthly-production-costs` | `getMonthlyProductionCosts` | Admin, Factory, Accounting |
| POST | `/api/v1/reports/exports/request` | `requestExport` | Admin, Accounting |
| GET | `/api/v1/reports/exports/{requestId}/download` | `downloadExport` | Admin, Accounting |

---

### Portal Module

| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/portal/dashboard` | `getDashboard` | Admin |
| GET | `/api/v1/portal/operations` | `getOperationsInsights` | Admin, Factory |
| GET | `/api/v1/portal/workforce` | `getWorkforceInsights` | Admin, HR |

---

### Auth Module

#### Authentication
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/auth/login` | `login` | Public |
| POST | `/api/v1/auth/refresh-token` | `refreshToken` | Public |
| POST | `/api/v1/auth/logout` | `logout` | Authenticated |
| GET | `/api/v1/auth/me` | `getCurrentUser` | Authenticated |
| POST | `/api/v1/auth/password/change` | `changePassword` | Authenticated |
| POST | `/api/v1/auth/password/forgot` | `forgotPassword` | Public |
| POST | `/api/v1/auth/password/forgot/superadmin` | `forgotSuperAdminPassword` | Public |
| POST | `/api/v1/auth/password/reset` | `resetPassword` | Public |

#### Profile
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/auth/profile` | `getProfile` | Authenticated |
| PUT | `/api/v1/auth/profile` | `updateProfile` | Authenticated |

#### MFA
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/auth/mfa/setup` | `setupMfa` | Authenticated |
| POST | `/api/v1/auth/mfa/activate` | `activateMfa` | Authenticated |
| POST | `/api/v1/auth/mfa/disable` | `disableMfa` | Authenticated |

---

### Admin Module

#### Users
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/admin/users` | `listUsers` | Admin |
| POST | `/api/v1/admin/users` | `createUser` | Admin |
| PUT | `/api/v1/admin/users/{id}` | `updateUser` | Admin |
| POST | `/api/v1/admin/users/{userId}/force-reset-password` | `forceResetPassword` | Admin |
| PUT | `/api/v1/admin/users/{userId}/status` | `updateUserStatus` | Admin |
| PATCH | `/api/v1/admin/users/{id}/suspend` | `suspendUser` | Admin |
| PATCH | `/api/v1/admin/users/{id}/unsuspend` | `unsuspendUser` | Admin |
| PATCH | `/api/v1/admin/users/{id}/mfa/disable` | `disableUserMfa` | Admin |
| DELETE | `/api/v1/admin/users/{id}` | `deleteUser` | Admin |

#### Settings
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/admin/settings` | `getSettings` | Admin |
| PUT | `/api/v1/admin/settings` | `updateSettings` | Admin |

#### Approvals & Exports
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| PUT | `/api/v1/admin/exports/{requestId}/approve` | `approveExport` | Admin |
| PUT | `/api/v1/admin/exports/{requestId}/reject` | `rejectExport` | Admin |
| POST | `/api/v1/admin/notify` | `sendNotification` | Admin |
| GET | `/api/v1/admin/approvals` | `listApprovals` | Admin |

#### Support Tickets
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/support/tickets` | `createTicket` | Admin |
| GET | `/api/v1/support/tickets` | `listTickets` | Admin |
| GET | `/api/v1/support/tickets/{ticketId}` | `getTicket` | Admin |

#### Changelog
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/changelog` | `listChangelog` | Authenticated |
| GET | `/api/v1/changelog/latest-highlighted` | `getLatestHighlighted` | Authenticated |

---

### Company Module

#### Multi-Company
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/multi-company/companies/switch` | `switchCompany` | Authenticated |

#### Company Management
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/companies` | `listCompanies` | Authenticated |
| DELETE | `/api/v1/companies/{id}` | `deleteCompany` | SuperAdmin |

#### SuperAdmin Operations
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/superadmin/dashboard` | `getSuperAdminDashboard` | SuperAdmin |
| GET | `/api/v1/superadmin/tenants` | `listTenants` | SuperAdmin |
| GET | `/api/v1/superadmin/tenants/{id}` | `getTenant` | SuperAdmin |
| PUT | `/api/v1/superadmin/tenants/{id}/lifecycle` | `updateTenantLifecycle` | SuperAdmin |
| PUT | `/api/v1/superadmin/tenants/{id}/limits` | `updateTenantLimits` | SuperAdmin |
| PUT | `/api/v1/superadmin/tenants/{id}/modules` | `updateTenantModules` | SuperAdmin |
| POST | `/api/v1/superadmin/tenants/{id}/support/warnings` | `addSupportWarning` | SuperAdmin |
| POST | `/api/v1/superadmin/tenants/{id}/support/admin-password-reset` | `resetAdminPassword` | SuperAdmin |
| PUT | `/api/v1/superadmin/tenants/{id}/support/context` | `updateSupportContext` | SuperAdmin |
| POST | `/api/v1/superadmin/tenants/{id}/force-logout` | `forceLogoutTenant` | SuperAdmin |
| PUT | `/api/v1/superadmin/tenants/{id}/admins/main` | `updateMainAdmin` | SuperAdmin |
| POST | `/api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/request` | `requestAdminEmailChange` | SuperAdmin |
| POST | `/api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/confirm` | `confirmAdminEmailChange` | SuperAdmin |

#### Tenant Onboarding
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/superadmin/tenants/coa-templates` | `listCoaTemplates` | SuperAdmin |
| POST | `/api/v1/superadmin/tenants/onboard` | `onboardTenant` | SuperAdmin |

#### Changelog (SuperAdmin)
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| POST | `/api/v1/superadmin/changelog` | `createChangelogEntry` | SuperAdmin |
| PUT | `/api/v1/superadmin/changelog/{id}` | `updateChangelogEntry` | SuperAdmin |
| DELETE | `/api/v1/superadmin/changelog/{id}` | `deleteChangelogEntry` | SuperAdmin |

---

### RBAC Module

| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/roles` | `listRoles` | Admin |
| GET | `/api/v1/roles/{id}` | `getRole` | Admin |
| POST | `/api/v1/roles` | `createRole` | Admin |
| PUT | `/api/v1/roles/{id}` | `updateRole` | Admin |
| DELETE | `/api/v1/roles/{id}` | `deleteRole` | Admin |

---

### Core Module

#### Enterprise Audit Trail
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/audit-trail` | `getAuditTrail` | Admin |
| GET | `/api/v1/audit-trail/events` | `listAuditEvents` | Admin |

#### Integration Health
| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| GET | `/api/integration/health` | `getIntegrationHealth` | Admin |

---

### Demo Module

| Method | Endpoint | Handler | Role Required |
|--------|----------|---------|---------------|
| * | `/api/v1/demo/*` | Various | Demo User |

---

## Event Consumers & Listeners

### Spring Event Listeners

| Listener | Event | Action |
|----------|-------|--------|
| `FactorySlipEventListener` | `PackagingSlipEvent` | Updates production log when packaging slip is created |
| `AccountingFacadeCore` | `InventoryMovementEvent` | Posts inventory accounting entries |
| `OrderAutoApprovalListener` | `SalesOrderCreatedEvent` | Triggers auto-approval workflow for qualifying orders |

### Event Publishing Points

| Publisher | Event | Trigger |
|-----------|-------|---------|
| `EventPublisherService` | `DomainEvent` | Outbox pattern for external system sync |
| `SalesService` | `SalesOrderCreatedEvent` | On sales order creation |
| `InventoryService` | `InventoryMovementEvent` | On stock movements |
| `InventoryService` | `InventoryValuationChangedEvent` | On valuation changes |
| `AccountingService` | `AccountCacheInvalidatedEvent` | On account changes |

---

## Scheduled Jobs

| Service | Method | Schedule | Description |
|---------|--------|----------|-------------|
| `DunningService` | `processDunningHoldRelease()` | `0 15 3 * * *` (3:15 AM daily) | Releases dunning holds after payment |
| `RefreshTokenService` | `cleanupExpiredTokens()` | `fixedDelay = 3600000` (1 hour) | Removes expired refresh tokens |
| `SupportTicketGitHubSyncService` | `syncTicketsWithGitHub()` | `0 */5 * * * *` (every 5 min) | Syncs support tickets with GitHub |
| `AuditDigestScheduler` | `generateDailyAuditDigest()` | `0 30 2 * * *` (2:30 AM daily) | Generates daily audit digest |
| `EnterpriseAuditTrailService` | `retryFailedEvents()` | `fixedDelay = 30000` (30 sec) | Retries failed audit trail events |
| `TokenBlacklistService` | `cleanupExpiredBlacklistedTokens()` | `fixedDelay = 3600000` (1 hour) | Cleans up expired blacklisted tokens |
| `SecurityMonitoringService` | `monitorSecurityEvents()` | `fixedDelay = 60000` (1 min) | Monitors security events |
| `SecurityMonitoringService` | `generateSecurityReport()` | `fixedDelay = 3600000` (1 hour) | Generates security reports |

---

## Filters

| Filter | Class | Order | Purpose |
|--------|-------|-------|---------|
| `JwtAuthenticationFilter` | `JwtAuthenticationFilter` | After Security Context | Validates JWT tokens, sets authentication |
| `CompanyContextFilter` | `CompanyContextFilter` | After Auth | Sets company/tenant context for request |
| `MustChangePasswordCorridorFilter` | `MustChangePasswordCorridorFilter` | After Auth | Enforces password change for users requiring it |

---

## Interceptors

| Interceptor | Class | Purpose |
|-------------|-------|---------|
| `TenantUsageMetricsInterceptor` | `TenantUsageMetricsInterceptor` | Tracks tenant usage metrics per request |
| `ModuleGatingInterceptor` | `ModuleGatingInterceptor` | Enforces module access restrictions |
| `TenantRuntimeEnforcementInterceptor` | `TenantRuntimeEnforcementInterceptor` | Enforces tenant runtime policies |

---

## Exception Handlers

| Handler | Class | Scope | Purpose |
|---------|-------|-------|---------|
| `GlobalExceptionHandler` | `GlobalExceptionHandler` | Application-wide | Handles all uncaught exceptions |
| `CoreFallbackExceptionHandler` | `CoreFallbackExceptionHandler` | Core module | Fallback exception handling |

---

## Endpoint Count by Module

| Module | Endpoints |
|--------|-----------|
| Accounting | ~50+ |
| Sales | ~30 |
| Purchasing | ~15 |
| Inventory | ~20 |
| Factory | ~20 |
| HR | ~25 |
| Invoice | ~5 |
| Production/Catalog | ~10 |
| Reports | ~20 |
| Admin | ~15 |
| Auth | ~12 |
| Company | ~20 |
| Portal | ~3 |
| Orchestrator | ~10 |
| RBAC | ~5 |
| **Total** | **~260+** |

---

## Security Role Matrix

| Role | Access Scope |
|------|--------------|
| `SuperAdmin` | Full system access, tenant management |
| `Admin` | Company-level admin, all modules |
| `Accounting` | Financial operations, reporting |
| `Sales` | Sales orders, dealers, promotions |
| `Purchasing` | Purchase orders, suppliers |
| `Inventory` | Stock management, adjustments |
| `Factory` | Production, packing, costing |
| `HR` | Employee management, payroll |
| `Dealer` | Dealer portal (own data only) |

---

## API Versioning

- Current version: `v1` (all endpoints under `/api/v1/`)
- Legacy endpoints: Some endpoints may have `X-` prefixed headers for backward compatibility
- Idempotency: Supports `Idempotency-Key` and legacy `X-Idempotency-Key` headers

---

## Notes

1. All endpoints require authentication unless marked as "Public"
2. Role requirements use Spring Security's `@PreAuthorize` annotations
3. `PortalRoleActionMatrix` provides centralized role constant definitions
4. All POST/PUT operations support idempotency via headers
5. Multi-tenant isolation is enforced via `CompanyContextFilter`
