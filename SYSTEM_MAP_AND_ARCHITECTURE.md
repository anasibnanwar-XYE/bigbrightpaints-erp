# BigBright ERP – System Map & Architecture

## 1. System Overview

**BigBright ERP** is a comprehensive, domain-driven ERP system built on Java 21 (Spring Boot 3.3.4) with PostgreSQL 16 and RabbitMQ. This is a **single consolidated backend** (erp-domain) that exposes all needed REST APIs for inventory, sales, purchasing, production manufacturing, accounting, HR/payroll, multi-company, RBAC authorization, dealer portals, and orchestrated workflows.

---

## 2. Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Persistence | Spring Data JPA (Hibernate 6), Flyway migrations |
| Database | PostgreSQL 16 |
| Messaging | Spring AMQP (RabbitMQ) |
| Security | Spring Security 6, JWT (jjwt 0.11.5), MFA/TOTP |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5, TestContainers (Postgres, RabbitMQ), Spring Test |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build Tool | Maven 3.9.x |
| Observability | Micrometer, OpenTelemetry, Spring Actuator |
| Scheduling | Spring Quartz, Spring Batch |
| Mapper | MapStruct 1.5.5 |

**Key Dependencies:**
- Optimistic locking via `@Version` (entity versioning)
- Retry logic via `@Retryable` for concurrent access
- Event-driven journal posting via `@EventListener`
- Transactional isolation (`REPEATABLE_READ` for accounting operations)

---

## 3. Main Modules (Domain Aggregates)

The codebase is organized into 15 modules under `com.bigbrightpaints.erp.modules`:

### 3.1 **accounting**
- **Purpose:** Chart of Accounts, Journal Entries, Double-entry bookkeeping, Account balances, Dealer/Supplier ledgers, Accounting periods.
- **Key Entities:**
  - `Account` (id, company_id, code, name, type, balance)
  - `JournalEntry` (id, company_id, reference_number, entry_date, status, dealer_id, supplier_id, accounting_period_id, reversal_of, correction_type, voided_at, posted_at, lines)
  - `JournalLine` (journal_entry_id, account_id, description, debit, credit)
  - `DealerLedgerEntry` (dealer_id, journal_entry_id, entry_date, reference_number, debit, credit)
  - `SupplierLedgerEntry` (supplier_id, journal_entry_id, entry_date, reference_number, debit, credit)
  - `AccountingPeriod` (company_id, fiscal_year, periodNumber, startDate, endDate, status, isYearEnd)
- **Services:**
  - `AccountingService` – core journal entry creation, ledger posting, dealer/supplier payment recording, journal reversal.
  - `AccountingFacade` – domain-specific facade exposing methods like `postSalesJournal`, `postPurchaseJournal`, `postCOGS`, `postMaterialConsumption`, `postCostAllocation`, `postInventoryAdjustment`, `postSalesReturn`, `postPurchaseReturn`, `postPayrollExpense`, etc.
  - `DealerLedgerService`, `SupplierLedgerService` – track AR/AP balances.
  - `ReconciliationService` – reconcile bank statements and payments.
  - `AccountingPeriodService` – manage fiscal periods, ensure period is open before posting.
  - `ReferenceNumberService` – generate journal reference numbers.
- **Flow:** Every business transaction (sales, purchase, production, payroll, payment) must post a `JournalEntry`. `AccountingFacade` acts as a central posting gateway called by other modules (sales, inventory, purchasing, production, HR).

---

### 3.2 **inventory**
- **Purpose:** Track raw materials and finished goods stock, batches, stock movements, adjustments, packaging slips, reservations.
- **Key Entities:**
  - `RawMaterial` (id, company_id, sku, name, unitType, currentStock, minStock, maxStock, reorderLevel, inventoryAccountId)
  - `RawMaterialBatch` (raw_material_id, batch_code, quantity, cost_per_unit, supplier_id, received_at)
  - `RawMaterialMovement` (raw_material_id, raw_material_batch_id, reference_type, reference_id, movement_type, quantity, unit_cost, journal_entry_id)
  - `FinishedGood` (id, company_id, product_code, name, unit, currentStock, reservedStock, costingMethod='FIFO', valuationAccountId, cogsAccountId, revenueAccountId, discountAccountId, taxAccountId)
  - `FinishedGoodBatch` (finished_good_id, batchCode, quantity, unitCost, manufacturedDate)
  - `InventoryMovement` (finished_good_id, batch_id, reference_type, reference_id, movement_type, quantity, unit_cost, journal_entry_id)
  - `InventoryAdjustment` (company_id, adjustment_type, reference, notes, journal_entry_id)
  - `InventoryAdjustmentLine` (adjustment_id, finished_good_id, batch_id, quantity_change, reason)
  - `InventoryReservation` (raw_material_id, reference_type, reference_id, quantity, status)
  - `PackagingSlip` (company_id, order_id, slip_number, slipped_date)
  - `PackagingSlipLine` (slip_id, order_item_id, quantity)
- **Services:**
  - `FinishedGoodsService` – manage finished goods CRUD, update stock, create batches for production receipts.
  - `RawMaterialService` – manage raw materials, update stock, record movements.
  - `InventoryAdjustmentService` – handle adjustments (shortage, overage, damage), post journal entries to variance account.
  - `BatchNumberService` – generate batch codes.
- **Flow:**
  - **Raw material purchase:** `RawMaterialService` creates batch, posts journal (Dr RM Inventory / Cr AP) via `AccountingFacade.postPurchaseJournal`.
  - **Production consumption:** `RawMaterialService` reduces stock, creates movement, posts journal (Dr WIP / Cr RM Inventory) via `AccountingFacade.postMaterialConsumption`.
  - **Finished goods receipt:** `FinishedGoodsService` creates batch, increases stock, posts journal (Dr FG Inventory / Cr WIP or Production Cost) via `AccountingFacade.postCostAllocation`.
  - **Sales dispatch:** `FinishedGoodsService` reduces stock, posts COGS (Dr COGS / Cr FG Inventory) via `AccountingFacade.postCOGS`.

---

### 3.3 **sales**
- **Purpose:** Manage dealers (customers), sales orders, order items, fulfillment, dispatch, invoicing, GST/Tax calculations, sales returns, dealer ledger.
- **Key Entities:**
  - `Dealer` (company_id, code, name, status, gstin, stateCode, email, phone, address, creditLimit, outstandingBalance, receivableAccountId, salesAccountId, discountAccountId, outputTaxAccountId)
  - `SalesOrder` (company_id, dealer_id, orderNumber, orderDate, status, subtotalAmount, gstTotal, gstTreatment, gstRate, gstRoundingAdjustment, totalAmount, paidAmount, paymentStatus, journalEntryId, items)
  - `SalesOrderItem` (order_id, finished_good_id, quantity, unitPrice, lineSubtotal, gstRate, gstAmount, lineTotal, dispatchQuantity)
  - ` dispatcher` events & packaging slips.
- **Services:**
  - `SalesService` – create/approve orders, manage order lifecycle, dispatch, payment.
  - `DealerService` – manage dealers, update outstanding balances.
  - `SalesJournalService` – post sales invoices (Dr AR / Cr Revenue + Output Tax) via `AccountingFacade.postSalesJournal`.
  - `SalesReturnService` – handle returns, reverse revenue & inventory.
  - `OrderNumberService` – generate order numbers.
- **Flow:**
  1. Create `SalesOrder` → status='PENDING'.
  2. Approve order → reserve inventory, post sales journal (Dr Dealer AR / Cr Revenue + Tax), update `journalEntryId`, status='APPROVED'.
  3. Dispatch → reduce stock, post COGS journal, create packaging slip.
  4. Receive payment → post receipt journal (Dr Cash / Cr Dealer AR) via `AccountingService.recordDealerReceipt`.

---

### 3.4 **purchasing**
- **Purpose:** Manage suppliers, raw material purchase invoices, purchase items, supplier ledger, supplier payments.
- **Key Entities:**
  - `Supplier` (company_id, code, name, status, email, phone, address, creditLimit, outstandingBalance, payableAccountId)
  - `RawMaterialPurchase` (company_id, supplier_id, invoiceNumber, invoiceDate, totalAmount, status, journal_entry_id)
  - `RawMaterialPurchaseItem` (purchase_id, raw_material_id, raw_material_batch_id, batch_code, quantity, cost_per_unit, lineTotal)
- **Services:**
  - `PurchasingService` – create purchase invoices, post purchase journal (Dr RM Inventory / Cr Supplier AP) via `AccountingFacade.postPurchaseJournal`.
  - `SupplierService` – manage suppliers, update outstanding balances.
- **Flow:**
  1. Record purchase → create `RawMaterialPurchase`, create `RawMaterialBatch`, update `RawMaterial.currentStock`.
  2. Post journal → Dr RM Inventory Accounts / Cr Supplier Payable.
  3. Payment → post payment journal (Dr Supplier AP / Cr Cash) via `AccountingService.recordSupplierPayment`.

---

### 3.5 **production**
- **Purpose:** Manufacturing execution, production logs, BOM/recipe management, material consumption, finished goods receipts, cost allocation (labor, overhead).
- **Key Entities:**
  - `ProductionProduct` (company_id, productCode, name, bomRecipe – JSON structure listing raw materials & quantities, laborCost, overheadCost)
  - `ProductionBrand` (company_id, brandCode, name, description)
  - `ProductionLog` (company_id, production_product_id, logCode, status, plannedQuantity, actualQuantity, productionDate, notes)
- **Services:**
  - `ProductionService` – create production logs, consume raw materials, receive finished goods, allocate costs.
- **Flow:**
  1. Start production → create `ProductionLog`.
  2. Consume raw materials → reduce `RawMaterial.currentStock`, post journal (Dr WIP / Cr RM Inventory) via `AccountingFacade.postMaterialConsumption`.
  3. Receive finished goods → increase `FinishedGood.currentStock`, create `FinishedGoodBatch`, post cost allocation journal (Dr FG Inventory / Cr Labor/Overhead Expense) via `AccountingFacade.postCostAllocation`.

---

### 3.6 **hr** (Payroll)
- **Purpose:** Employee management, attendance, payroll run calculation, payroll journal posting.
- **Key Entities:**
  - `Employee` (company_id, employeeCode, name, email, department, designation, salary, status)
  - `PayrollRun` (company_id, period, runDate, totalGrossPay, totalNetPay, status, journal_entry_id)
  - `PayrollRunItem` (payroll_run_id, employee_id, grossPay, deductions, netPay)
- **Services:**
  - `PayrollService` – run payroll, calculate gross/net, post expense journal (Dr Payroll Expense / Cr Payroll Payable) via `AccountingService.recordPayrollPayment`.
- **Flow:**
  1. Create `PayrollRun` for period.
  2. Calculate gross/net for all employees.
  3. Post journal → Dr Payroll Expense / Cr Payroll Payable (or Cash if paid immediately).

---

### 3.7 **auth**
- **Purpose:** User authentication, JWT token issuance, MFA (TOTP), password management, recovery codes.
- **Key Entities:**
  - `User` (username, email, passwordHash, mfaEnabled, mfaSecret, failedLoginAttempts, accountLocked, lastPasswordChange)
  - `MfaRecoveryCode` (user_id, codeHash, usedAt)
  - `PasswordResetToken` (user_id, token, expiresAt, usedAt)
  - `PasswordHistory` (user_id, passwordHash, createdAt)
- **Services:**
  - `AuthService` – login, MFA verification, token generation.
  - `MfaService` – setup, activate, disable MFA.
  - `PasswordResetService` – request token, reset password.
- **Flow:** Login → verify credentials → verify MFA code (if enabled) → issue JWT.

---

### 3.8 **company**
- **Purpose:** Multi-company/multi-tenant isolation, company settings, accounting defaults (GST, payroll accounts).
- **Key Entities:**
  - `Company` (id, code, name, gstin, stateCode, fiscalYearStart, taxRateDefault, accountingSettings – JSON)
- **Services:**
  - `CompanyService` – manage companies, switch context.
  - `CompanyContextService` – resolve current company from security context.

---

### 3.9 **rbac**
- **Purpose:** Role-Based Access Control, permissions, role assignment.
- **Key Entities:**
  - `Role` (name, description)
  - `Permission` (name, description)
  - `RolePermission` (role_id, permission_id)
  - `UserRole` (user_id, role_id, company_id)
- **Services:**
  - `RbacService` – check permissions, assign roles.

---

### 3.10 **admin**
- **Purpose:** Admin console operations, user management.
- **Services:**
  - `AdminService` – manage users, roles, global settings.

---

### 3.11 **portal**
- **Purpose:** Dealer self-service portal (view orders, ledger, make payments).
- **Services:**
  - `DealerPortalService`

---

### 3.12 **invoice**
- **Purpose:** Invoice generation, linking journal entries to invoices.
- **Key Entities:**
  - `Invoice` (company_id, invoiceNumber, invoiceDate, dealer_id, totalAmount, journal_entry_id)

---

### 3.13 **reports**
- **Purpose:** Financial reports (Trial Balance, Profit & Loss, Balance Sheet, GST reports, Ledger statements).
- **Services:**
  - `ReportService` – generate reports from journal_entries, accounts.

---

### 3.14 **factory**
- **Purpose:** Factory-specific workflows, batch dispatch, batch tracking.

---

### 3.15 **demo**
- **Purpose:** Demo data seeding.

---

## 4. Data Flow Mappings

### 4.1 Journal Entry Creation Pathways

The `AccountingFacade` provides specialized methods called by domain services to post journals:

| Transaction Type | Called by Module | Facade Method | Debits | Credits |
|------------------|------------------|---------------|--------|---------|
| **Sales Invoice** | sales | `postSalesJournal` | Dealer AR | Revenue, Output Tax |
| **Sales Return** | sales | `postSalesReturn` | Revenue, Output Tax | Dealer AR |
| **Purchase Invoice** | purchasing | `postPurchaseJournal` | RM Inventory | Supplier AP |
| **Purchase Return** | purchasing | `postPurchaseReturn` | Supplier AP | RM Inventory |
| **Dealer Payment** | sales | `recordDealerReceipt` | Cash/Bank | Dealer AR |
| **Supplier Payment** | purchasing | `recordSupplierPayment` | Supplier AP | Cash/Bank |
| **Material Consumption** | production | `postMaterialConsumption` | WIP | RM Inventory |
| **Cost Allocation** | production | `postCostAllocation` | FG Inventory | Labor, Overhead Expense |
| **COGS (Sales Dispatch)** | sales/inventory | `postCOGS` | COGS | FG Inventory |
| **Inventory Adjustment** | inventory | `postInventoryAdjustment` | Inventory or Variance | Variance or Inventory |
| **Payroll Expense** | hr | `recordPayrollPayment` | Payroll Expense | Payroll Payable or Cash |

**Core Posting Logic:**
- All journal posting goes through `AccountingService.createJournalEntry(JournalEntryRequest)`.
- This method:
  1. Validates company context.
  2. Validates accounting period is open (via `AccountingPeriodService`).
  3. Validates total debits = total credits (tolerance: 0.01).
  4. Generates reference number if not provided.
  5. Creates `JournalEntry` entity with status='POSTED'.
  6. Creates `JournalLine` entities for each debit/credit.
  7. Updates `Account.balance` for each affected account (optimistic locking via `@Version`).
  8. If dealer/supplier involved, creates `DealerLedgerEntry` or `SupplierLedgerEntry`.
  9. Persists all entities in a single transaction (isolation=REPEATABLE_READ for AccountingFacade methods).
  10. Returns `JournalEntryDto`.

---

### 4.2 Inventory Stock Update Flow

**Raw Material Stock:**
1. **Purchase:** `RawMaterialService.recordPurchase` → creates `RawMaterialBatch`, increases `RawMaterial.currentStock`, creates `RawMaterialMovement` (type='PURCHASE'), posts journal via `AccountingFacade.postPurchaseJournal`.
2. **Consumption (Production):** `ProductionService.consumeMaterials` → reduces `RawMaterial.currentStock`, creates `RawMaterialMovement` (type='CONSUMPTION'), posts journal via `AccountingFacade.postMaterialConsumption`.
3. **Adjustment:** `RawMaterialService.adjust` → updates `RawMaterial.currentStock`, creates `RawMaterialMovement` (type='ADJUSTMENT'), posts journal via `AccountingFacade.postInventoryAdjustment`.

**Finished Goods Stock:**
1. **Production Receipt:** `FinishedGoodsService.receiveProduction` → creates `FinishedGoodBatch`, increases `FinishedGood.currentStock`, creates `InventoryMovement` (type='RECEIPT'), posts journal via `AccountingFacade.postCostAllocation`.
2. **Sales Dispatch:** `SalesService.dispatch` → reduces `FinishedGood.currentStock`, creates `InventoryMovement` (type='DISPATCH'), posts COGS journal via `AccountingFacade.postCOGS`.
3. **Sales Return:** `SalesReturnService.processReturn` → increases `FinishedGood.currentStock`, creates `InventoryMovement` (type='RETURN'), reverses COGS journal.
4. **Adjustment:** `InventoryAdjustmentService.adjust` → updates `FinishedGood.currentStock`, creates `InventoryMovement` (type='ADJUSTMENT'), posts journal via `AccountingFacade.postInventoryAdjustment`.

---

### 4.3 Ledger Balance Update Flow

**Account Balances:**
- `Account.balance` is a **materialized balance** stored on the `accounts` table.
- Upon posting a journal entry, `AccountingService.createJournalEntry`:
  - Builds a map of account deltas (summing debits and credits from all journal lines).
  - For each affected account:
    - `account.setBalance(currentBalance + delta)` where delta = (debit - credit) if debit-normal account, else (credit - debit).
  - Saves all affected accounts via `accountRepository.saveAll(accountDeltas.keySet())`.
- Optimistic locking (`@Version`) prevents concurrent balance corruption.

**Dealer/Supplier Balances:**
- `Dealer.outstandingBalance` and `Supplier.outstandingBalance` are **materialized balances**.
- Updated by `DealerLedgerService` and `SupplierLedgerService` when journal entries are posted:
  - `DealerLedgerService.recordEntry` → creates `DealerLedgerEntry`, updates `Dealer.outstandingBalance`.
  - `SupplierLedgerService.recordEntry` → creates `SupplierLedgerEntry`, updates `Supplier.outstandingBalance`.

---

### 4.4 Tax (GST) Handling

**Sales:**
- `SalesOrder` has fields: `subtotalAmount`, `gstTotal`, `gstTreatment` (NONE, INTRA_STATE, INTER_STATE), `gstRate`, `gstRoundingAdjustment`.
- `SalesOrderItem` has: `lineSubtotal`, `gstRate`, `gstAmount`, `lineTotal`.
- When posting sales journal:
  - Debits: Dealer AR (full totalAmount including tax).
  - Credits: Revenue account (subtotalAmount), Output Tax account (gstTotal).
- Tax rate is configurable per company (`Company.taxRateDefault`) and can be overridden per item.

**Purchase:**
- Purchases typically record Input Tax in a separate account (if GST system).
- Currently, purchase journal only posts to RM Inventory and Supplier AP; Input Tax handling may be incomplete.

**Tax Reporting:**
- `ReportService` can query `journal_entries` and `journal_lines` filtered by tax accounts to generate GSTR-like reports.

---

## 5. Orchestration Layer

**Module:** `orchestrator`
- **Purpose:** High-level business workflows that coordinate multiple modules.
- **Services:**
  - `IntegrationCoordinator` – orchestrates order approval, factory dispatch, payroll run.
- **Example Workflows:**
  - `POST /api/v1/orchestrator/orders/{id}/approve`: reserves inventory, queues production (if needed), posts sales journal, emits outbox event for downstream systems.
  - `POST /api/v1/orchestrator/factory/dispatch/{batchId}`: marks batches as dispatched, posts COGS journal, updates stock.
  - `POST /api/v1/orchestrator/payroll/run`: syncs HR data, creates payroll run, posts payroll journal.

---

## 6. Accounting Module Entry Points

**Journal Entries are created at:**

1. **Sales Module:**
   - `SalesJournalService.postSalesInvoice` → calls `AccountingFacade.postSalesJournal`.
   - `SalesReturnService.processReturn` → calls `AccountingFacade.postSalesReturn`.
   - `SalesService.recordPayment` → calls `AccountingService.recordDealerReceipt`.

2. **Purchasing Module:**
   - `PurchasingService.recordPurchase` → calls `AccountingFacade.postPurchaseJournal`.
   - `PurchasingService.recordPayment` → calls `AccountingService.recordSupplierPayment`.

3. **Production Module:**
   - `ProductionService.consumeMaterials` → calls `AccountingFacade.postMaterialConsumption`.
   - `ProductionService.receiveFinished Goods` → calls `AccountingFacade.postCostAllocation`.

4. **Inventory Module:**
   - `InventoryAdjustmentService.adjust` → calls `AccountingFacade.postInventoryAdjustment`.
   - `FinishedGoodsService.dispatch` → calls `AccountingFacade.postCOGS`.

5. **HR Module:**
   - `PayrollService.runPayroll` → calls `AccountingService.recordPayrollPayment`.

6. **Manual Journal Entries:**
   - `AccountingController.createJournalEntry` → calls `AccountingService.createJournalEntry` directly for manual adjustments.

**Event‑Driven Posting:**
- `AccountingFacade` has `@EventListener` methods that listen for domain events (e.g., `SalesOrderApprovedEvent`, `ProductionCompletedEvent`) and automatically post journal entries.

---

## 7. Inventory Module Entry Points

**Stock Quantities are updated at:**

1. **Raw Material Purchase:**
   - `RawMaterialService.recordPurchase` → updates `RawMaterial.currentStock`, creates `RawMaterialBatch`, posts journal.

2. **Production Consumption:**
   - `ProductionService.consumeMaterials` → reduces `RawMaterial.currentStock`, posts journal.

3. **Finished Goods Receipt:**
   - `FinishedGoodsService.receiveProduction` → increases `FinishedGood.currentStock`, creates batch, posts journal.

4. **Sales Dispatch:**
   - `SalesService.dispatch` → reduces `FinishedGood.currentStock`, posts COGS journal.

5. **Inventory Adjustments:**
   - `InventoryAdjustmentService.adjust` → updates stock (raw or finished), posts variance journal.

**Reservation System:**
- `InventoryReservation` entity tracks pending reservations (e.g., when sales order is approved but not yet dispatched).
- `FinishedGood.reservedStock` field tracks total reserved quantity.

---

## 8. Where Journal Entries are Created (Summary)

| Service | Method | Journal Type | Trigger |
|---------|--------|--------------|---------|
| `AccountingFacade` | `postSalesJournal` | Sales Invoice | Sales order approval |
| `AccountingFacade` | `postPurchaseJournal` | Purchase Invoice | Raw material purchase |
| `AccountingFacade` | `postCOGS` | COGS | Sales dispatch |
| `AccountingFacade` | `postMaterialConsumption` | Material Consumption | Production material issue |
| `AccountingFacade` | `postCostAllocation` | Cost Allocation | Production FG receipt |
| `AccountingFacade` | `postInventoryAdjustment` | Inventory Adjustment | Stock adjustment |
| `AccountingFacade` | `postSalesReturn` | Sales Return | Sales return |
| `AccountingFacade` | `postPurchaseReturn` | Purchase Return | Purchase return |
| `AccountingService` | `recordDealerReceipt` | Dealer Payment | Customer payment |
| `AccountingService` | `recordSupplierPayment` | Supplier Payment | Vendor payment |
| `AccountingService` | `recordPayrollPayment` | Payroll Expense | Payroll run |
| `AccountingService` | `createJournalEntry` | Manual Entry | Admin manual posting |

---

## 9. Where Ledger Balances are Updated

**Account Balances (`accounts.balance`):**
- **Updated in:** `AccountingService.createJournalEntry` → after journal lines are saved, account deltas are computed and applied to `Account.balance`, then `accountRepository.saveAll()`.

**Dealer Outstanding Balances (`dealers.outstandingBalance`):**
- **Updated in:** `DealerLedgerService.recordEntry` → called from `AccountingService.createJournalEntry` when journal entry involves a dealer.

**Supplier Outstanding Balances (`suppliers.outstandingBalance`):**
- **Updated in:** `SupplierLedgerService.recordEntry` → called from `AccountingService.createJournalEntry` when journal entry involves a supplier.

---

## 10. Where Stock Quantities are Updated

**Raw Material Stock (`raw_materials.current_stock`):**
- **Increased:** `RawMaterialService.recordPurchase`
- **Decreased:** `ProductionService.consumeMaterials`, `RawMaterialService.adjustStock`
- **Movement Records:** `raw_material_movements` table tracks all IN/OUT with reference to source transaction & journal.

**Finished Goods Stock (`finished_goods.current_stock`, `finished_goods.reserved_stock`):**
- **Increased:** `FinishedGoodsService.receiveProduction`, `SalesReturnService.processReturn`
- **Decreased:** `SalesService.dispatch`, `InventoryAdjustmentService.reduceStock`
- **Reserved:** `SalesService.approveOrder` (increases `reservedStock`), `SalesService.dispatch` (decreases `reservedStock`).
- **Movement Records:** `inventory_movements` table tracks all IN/OUT with reference to source transaction & journal.

---

## 11. Where Taxes (GST) are Computed & Stored

**Sales Tax:**
- **Computed in:** `SalesService.createOrder` or `SalesService.approveOrder` → calculates `gstAmount` per item based on `gstRate`, sums to `SalesOrder.gstTotal`.
- **Posted to:** Output Tax account (specified in `Dealer.outputTaxAccountId` or `FinishedGood.taxAccountId`).
- **Journal:** Sales journal credits Output Tax account for `gstTotal`.

**Purchase Tax (Input Tax):**
- **Not fully implemented.** Current purchase journal only posts to RM Inventory and Supplier AP. Input Tax handling would require:
  - `RawMaterialPurchase` to have `gstTotal` field.
  - Purchase journal to credit Input Tax account separately.

**Tax Reporting:**
- `ReportService` can query `journal_lines` filtered by tax account codes to generate tax liability reports.

---

## 12. Chart of Accounts Modeling

**Entity:** `Account`
- **Fields:** code, name, type (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE, COGS), balance.
- **Types Enum:**
  ```java
  public enum AccountType {
      ASSET,        // Dr normal
      LIABILITY,    // Cr normal
      EQUITY,       // Cr normal
      REVENUE,      // Cr normal
      EXPENSE,      // Dr normal
      COGS;         // Dr normal (special expense)
  }
  ```
- **Standard Accounts (expected):**
  - ASSET: AR (Accounts Receivable – Debtors), Inventory (RM & FG), Cash, Bank, WIP.
  - LIABILITY: AP (Accounts Payable – Creditors), Payroll Payable, Tax Payable (GST Output).
  - EQUITY: Retained Earnings, Owner's Equity.
  - REVENUE: Sales Revenue, Service Revenue.
  - EXPENSE: Payroll Expense, Labor, Overhead, Variance (inventory).
  - COGS: Cost of Goods Sold.

**Account Configuration:**
- Each `Dealer` has fields: `receivableAccountId`, `salesAccountId`, `discountAccountId`, `outputTaxAccountId`.
- Each `Supplier` has: `payableAccountId`.
- Each `FinishedGood` has: `valuationAccountId`, `cogsAccountId`, `revenueAccountId`, `discountAccountId`, `taxAccountId`.
- Each `RawMaterial` has: `inventoryAccountId`.

**Missing Standard Accounts (potential gaps):**
- No explicit Input Tax account configuration for purchases.
- No depreciation, bad debts, or accrual accounts visible yet.

---

## 13. Database Schema Highlights

**Flyway Migrations (40 files):**
- V1: Core tables (companies, users, roles).
- V2: Orchestrator tables.
- V3: Inventory tables (raw_materials, raw_material_batches, inventory_reservations).
- V4: Sales tables (dealers, sales_orders, sales_order_items).
- V5: Accounting tables (accounts, journal_entries, journal_lines).
- V6: Factory tables.
- V7: HR tables (employees, payroll_runs).
- V8–V10: Sales fulfillment, finished goods.
- V11: Finished goods account links.
- V12: Invoices.
- V13: MFA support.
- V14–V16: User profiles, roles.
- V17: Dealer portal.
- V18–V20: Production catalog, logs, pricing.
- V21: Sales GST and account fields.
- V22: Dealer ledger entries.
- V23: Password history.
- V24–V26: Order states, invoice-journal link.
- V27: Raw material purchasing (suppliers, supplier_ledger_entries, raw_material_purchases, raw_material_movements).
- V28: Payroll run totals.
- V29: Inventory movement journal link.
- V30: Company payroll account defaults.
- V31: Raw material inventory account.
- V32: MFA recovery codes.
- V33: Audit logging.
- V34: Company GST defaults.
- V35: Entity versioning (`@Version` for optimistic locking).
- V36: Number sequences.
- V37: Password reset tokens.
- V38: Consolidate roles.
- V39: Accounting periods.
- V40: Outbox retry backoff.

**Key Constraints:**
- `UNIQUE(company_id, code)` on accounts, dealers, suppliers, raw_materials, finished_goods → multi-tenant isolation.
- `UNIQUE(company_id, reference_number)` on journal_entries → idempotency.
- Foreign keys: ON DELETE CASCADE for child records, ON DELETE RESTRICT for critical references (e.g., accounts in journal_lines).

---

## 14. Event-Driven Architecture

**Outbox Pattern:**
- Detected in V40 (outbox retry backoff) suggests transactional outbox for guaranteed event delivery.

**Event Listeners:**
- `AccountingFacade` has `@EventListener` methods to auto-post journals on domain events.

**RabbitMQ:**
- Configured via Spring AMQP; likely used for async workflows, outbox processing, inter-service events.

---

## 15. Security & Authorization

**Multi‑Tenancy:**
- All core entities scoped by `company_id`.
- `CompanyContextService` provides current company from JWT claims.

**RBAC:**
- `UserRole`, `Role`, `Permission`, `RolePermission` tables.
- Security checks via `@PreAuthorize` annotations (Spring Security).

**MFA:**
- TOTP-based MFA, recovery codes.

---

## 16. Testing Infrastructure

**TestContainers:**
- Spins up PostgreSQL 16 and RabbitMQ for integration tests.

**Test Types:**
- Unit tests (services, domain logic).
- Integration tests (repository, transaction, API).
- Potential for end-to-end tests via Cypress (cypress-e2e-tests directory).

---

## 17. Conclusion

This ERP follows a **domain-driven, event-sourced accounting model** where:
- Every business transaction triggers a **double-entry journal posting** via `AccountingFacade`.
- **Account balances** and **ledger balances** (dealer/supplier AR/AP) are **materialized** and updated transactionally.
- **Inventory stock** is tracked in real-time with batch-level costing (FIFO).
- **Tax (GST)** is computed on sales and recorded in tax accounts.
- **Accounting periods** enforce period-based posting restrictions.
- **Optimistic locking** and **retry logic** handle concurrency.
- **Multi-company** support with strict tenant isolation.
- **RBAC** and **MFA** provide enterprise-grade security.

The architecture is **modular**, **transactional**, and **audit-ready**, with comprehensive traceability from every stock movement and ledger posting back to source documents (orders, invoices, production logs).

---
