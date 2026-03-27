# Database Relationships

This document maps entity relationships in BigBrightPaints ERP based on JPA annotations (`@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany`) found in the domain entities.

## Entity Relationship Overview

```
                                    ┌─────────────┐
                                    │   Company   │
                                    │  (Tenant)   │
                                    └──────┬──────┘
                                           │
        ┌──────────────────────────────────┼──────────────────────────────────┐
        │                                  │                                  │
        ▼                                  ▼                                  ▼
┌───────────────┐                 ┌───────────────┐                 ┌───────────────┐
│  UserAccount  │                 │    Dealer     │                 │   Supplier    │
│   (Auth)      │                 │   (Sales)     │                 │ (Purchasing)  │
└───────┬───────┘                 └───────┬───────┘                 └───────┬───────┘
        │                                 │                                 │
        │         ┌───────────────────────┼───────────────────────┐         │
        │         │                       │                       │         │
        ▼         ▼                       ▼                       ▼         ▼
┌───────────────────┐            ┌───────────────────┐    ┌───────────────────┐
│ SalesOrder        │            │   Invoice         │    │  PurchaseOrder    │
│ • Dealer ─────────┼────────────┤   • Dealer        │    │  • Supplier       │
│ • Items[]         │            │   • Lines[]       │    │  • Lines[]        │
└───────────────────┘            │   • SalesOrder    │    └───────────────────┘
                                 └───────────────────┘             │
                                                                   ▼
                                                          ┌───────────────────┐
                                                          │  GoodsReceipt     │
                                                          │  • Supplier       │
                                                          │  • PurchaseOrder  │
                                                          │  • Lines[]        │
                                                          └───────────────────┘
```

---

## Core Entity: Company (Tenant)

All entities are tenant-scoped through `Company`.

```java
@Entity
class Company {
    @ManyToOne Account receivableAccount;
    @ManyToOne Account payableAccount;
}
```

**Relationships:**
- `Company` → `Account` (optional default accounts)

---

## Accounting Module Entities

### Account
```
Account
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) AccountType accountType
└── @ManyToOne Account parentAccount (hierarchy)
```

### JournalEntry
```
JournalEntry
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) AccountingPeriod period
├── @ManyToOne Account reversalOf (for reversals)
├── @ManyToOne JournalReferenceMapping referenceMapping
├── @ManyToOne PayrollRun payrollRun
├── @ManyToOne TallyImport tallyImport
├── @ManyToOne OpeningBalanceImport openingBalanceImport
├── @OneToOne JournalEntry reversalEntry (reverse side)
└── @OneToMany JournalLine[] lines
```

### JournalLine
```
JournalLine
├── @ManyToOne(optional=false) JournalEntry journalEntry
└── @ManyToOne(optional=false) Account account
```

### AccountingPeriod
```
AccountingPeriod
├── @ManyToOne(optional=false) Company company
└── (snapshots, trial balance lines via repositories)
```

### DealerLedgerEntry
```
DealerLedgerEntry
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Dealer dealer
├── @ManyToOne JournalEntry journalEntry (optional)
└── @ManyToOne Invoice invoice (optional, source document)
```

### SupplierLedgerEntry
```
SupplierLedgerEntry
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Supplier supplier
├── @ManyToOne JournalEntry journalEntry (optional)
└── @ManyToOne RawMaterialPurchase purchase (optional)
```

### PartnerSettlementAllocation
```
PartnerSettlementAllocation
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) JournalEntry journalEntry
├── @ManyToOne Invoice invoice
├── @ManyToOne RawMaterialPurchase purchase
├── @ManyToOne Dealer dealer
├── @ManyToOne Supplier supplier
└── (links settlements to original documents)
```

### BankReconciliationSession
```
BankReconciliationSession
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Account bankAccount
├── @ManyToOne AccountingPeriod period
└── @OneToMany BankReconciliationItem[] items
```

### BankReconciliationItem
```
BankReconciliationItem
├── @ManyToOne(optional=false) BankReconciliationSession session
├── @ManyToOne(optional=false) JournalEntry journalEntry
├── @ManyToOne(optional=false) JournalLine journalLine
└── (bank statement line matching)
```

### ClosedPeriodPostingException
```
ClosedPeriodPostingException
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) AccountingPeriod period
└── @ManyToOne JournalEntry journalEntry
```

### TallyImport
```
TallyImport
├── @ManyToOne(optional=false) Company company
└── (import tracking)
```

### ReconciliationDiscrepancy
```
ReconciliationDiscrepancy
├── @ManyToOne(optional=false) Company company
├── @ManyToOne BankReconciliationSession session
└── @ManyToOne JournalEntry journalEntry
```

### AccountingEvent
```
AccountingEvent
├── @ManyToOne Company company
└── (event store for audit trail)
```

---

## Sales Module Entities

### Dealer
```
Dealer
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Account receivableAccount
├── @ManyToOne Account creditLimitOverride
└── @ManyToOne Account gstAccount
```

### SalesOrder
```
SalesOrder
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Dealer dealer
├── @ManyToOne Account shipToAccount (optional)
└── @OneToMany SalesOrderItem[] items
```

### SalesOrderItem
```
SalesOrderItem
└── @ManyToOne(optional=false) SalesOrder salesOrder
```

### SalesOrderStatusHistory
```
SalesOrderStatusHistory
├── @ManyToOne(optional=false) SalesOrder salesOrder
└── @ManyToOne(optional=false) UserAccount changedBy
```

### CreditLimitOverrideRequest
```
CreditLimitOverrideRequest
├── @ManyToOne(optional=false) Company company
├── @ManyToOne Dealer dealer
├── @ManyToOne Account account
├── @ManyToOne UserAccount requestedBy
└── @ManyToOne UserAccount reviewedBy
```

### CreditRequest
```
CreditRequest
├── @ManyToOne(optional=false) Dealer dealer
└── @ManyToOne UserAccount reviewedBy (optional)
```

### Promotion
```
Promotion
└── @ManyToOne(optional=false) Company company
```

### SalesTarget
```
SalesTarget
└── @ManyToOne(optional=false) Company company
```

### OrderSequence
```
OrderSequence
└── @ManyToOne(optional=false) Company company
```

---

## Invoice Module Entities

### Invoice
```
Invoice
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Dealer dealer
├── @ManyToOne(optional=false) InvoiceSequence sequence
├── @ManyToOne SalesOrder salesOrder (optional)
├── @ManyToOne JournalEntry journalEntry (optional, after posting)
└── @OneToMany InvoiceLine[] lines
```

### InvoiceLine
```
InvoiceLine
└── @ManyToOne(optional=false) Invoice invoice
```

### InvoiceSequence
```
InvoiceSequence
└── @ManyToOne(optional=false) Company company
```

---

## Purchasing Module Entities

### Supplier
```
Supplier
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Account payableAccount
└── @ManyToOne Account gstAccount (optional)
```

### PurchaseOrder
```
PurchaseOrder
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Supplier supplier
└── @OneToMany PurchaseOrderLine[] lines
```

### PurchaseOrderLine
```
PurchaseOrderLine
├── @ManyToOne(optional=false) PurchaseOrder purchaseOrder
└── @ManyToOne(optional=false) RawMaterial rawMaterial
```

### PurchaseOrderStatusHistory
```
PurchaseOrderStatusHistory
├── @ManyToOne(optional=false) PurchaseOrder purchaseOrder
└── @ManyToOne(optional=false) UserAccount changedBy
```

### GoodsReceipt
```
GoodsReceipt
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Supplier supplier
├── @ManyToOne(optional=false) PurchaseOrder purchaseOrder
└── @OneToMany GoodsReceiptLine[] lines
```

### GoodsReceiptLine
```
GoodsReceiptLine
├── @ManyToOne(optional=false) GoodsReceipt goodsReceipt
├── @ManyToOne(optional=false) RawMaterial rawMaterial
└── @ManyToOne RawMaterialBatch batch (optional)
```

### RawMaterialPurchase
```
RawMaterialPurchase
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) Supplier supplier
├── @ManyToOne(optional=false) JournalEntry journalEntry (after posting)
├── @ManyToOne PurchaseOrder purchaseOrder
├── @ManyToOne GoodsReceipt goodsReceipt
├── @ManyToOne Account payableAccount
└── @OneToMany RawMaterialPurchaseLine[] lines
```

### RawMaterialPurchaseLine
```
RawMaterialPurchaseLine
├── @ManyToOne(optional=false) RawMaterialPurchase purchase
├── @ManyToOne(optional=false) RawMaterial rawMaterial
└── @ManyToOne RawMaterialBatch batch (optional)
```

---

## Inventory Module Entities

### FinishedGood
```
FinishedGood
├── @ManyToOne(optional=false) Company company
└── @ManyToOne(optional=false) ProductionProduct product (SKU)
```

### FinishedGoodBatch
```
FinishedGoodBatch
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) FinishedGood finishedGood
├── @ManyToOne InventoryBatchSource source (optional)
└── @ManyToOne JournalEntry journalEntry (optional, cost posting)
```

### RawMaterial
```
RawMaterial
└── @ManyToOne(optional=false) Company company
```

### RawMaterialBatch
```
RawMaterialBatch
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) RawMaterial rawMaterial
└── @ManyToOne JournalEntry journalEntry (optional)
```

### InventoryMovement
```
InventoryMovement
├── @ManyToOne(optional=false) Company company
├── @ManyToOne FinishedGoodBatch batch (optional)
└── (tracks stock movements)
```

### RawMaterialMovement
```
RawMaterialMovement
├── @ManyToOne(optional=false) Company company
├── @ManyToOne RawMaterialBatch batch (optional)
└── @ManyToOne JournalEntry journalEntry (optional)
```

### PackagingSlip
```
PackagingSlip
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) FinishedGoodBatch batch
└── @OneToMany PackagingSlipLine[] lines
```

### PackagingSlipLine
```
PackagingSlipLine
├── @ManyToOne(optional=false) PackagingSlip packagingSlip
└── @ManyToOne(optional=false) FinishedGoodBatch finishedGoodBatch
```

### InventoryAdjustment
```
InventoryAdjustment
├── @ManyToOne(optional=false) Company company
└── @OneToMany InventoryAdjustmentLine[] lines
```

### InventoryAdjustmentLine
```
InventoryAdjustmentLine
├── @ManyToOne(optional=false) InventoryAdjustment adjustment
└── @ManyToOne(optional=false) FinishedGood finishedGood
```

### RawMaterialAdjustment
```
RawMaterialAdjustment
├── @ManyToOne(optional=false) Company company
└── @OneToMany RawMaterialAdjustmentLine[] lines
```

### RawMaterialAdjustmentLine
```
RawMaterialAdjustmentLine
├── @ManyToOne(optional=false) RawMaterialAdjustment adjustment
└── @ManyToOne(optional=false) RawMaterial rawMaterial
```

### InventoryReservation
```
InventoryReservation
├── @ManyToOne Company company
├── @ManyToOne FinishedGoodBatch batch (optional)
├── @ManyToOne SalesOrder salesOrder
└── @ManyToOne PackagingSlip packagingSlip
```

### RawMaterialIntakeRecord
```
RawMaterialIntakeRecord
└── @ManyToOne(optional=false) Company company
```

### OpeningStockImport
```
OpeningStockImport
└── @ManyToOne(optional=false) Company company
```

---

## Factory Module Entities

### ProductionLog
```
ProductionLog
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) FinishedGoodBatch finishedGoodBatch
├── @ManyToOne(optional=false) ProductionBatch productionBatch
├── @ManyToOne(optional=false) ProductionPlan productionPlan
├── @OneToMany ProductionLogMaterial[] materials
└── @OneToMany PackingRecord[] packingRecords
```

### ProductionLogMaterial
```
ProductionLogMaterial
├── @ManyToOne(optional=false) ProductionLog log
├── @ManyToOne(optional=false) RawMaterial rawMaterial
├── @ManyToOne RawMaterialBatch batch (optional)
└── @ManyToOne RawMaterialMovement movement (optional)
```

### ProductionBatch
```
ProductionBatch
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) FinishedGoodBatch finishedGoodBatch
└── @ManyToOne ProductionLog productionLog (optional)
```

### ProductionPlan
```
ProductionPlan
└── @ManyToOne(optional=false) Company company
```

### FactoryTask
```
FactoryTask
└── @ManyToOne(optional=false) Company company
```

### PackingRecord
```
PackingRecord
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) ProductionLog productionLog
├── @ManyToOne(optional=false) FinishedGoodBatch sourceBatch
├── @ManyToOne(optional=false) FinishedGoodBatch targetBatch
├── @ManyToOne ProductionPlan productionPlan (optional)
├── @ManyToOne JournalEntry journalEntry (optional)
└── @ManyToOne AccountingPeriod period (optional)
```

### PackagingSizeMapping
```
PackagingSizeMapping
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) ProductionProduct bulkProduct
└── @ManyToOne(optional=false) ProductionProduct packedProduct
```

### SizeVariant
```
SizeVariant
├── @ManyToOne(optional=false) Company company
└── @ManyToOne(optional=false) ProductionProduct product
```

### PackingRequestRecord
```
PackingRequestRecord
└── @ManyToOne(optional=false) Company company
```

---

## Production/Catalog Module Entities

### ProductionProduct
```
ProductionProduct
├── @ManyToOne(optional=false) Company company
└── @ManyToOne(optional=false) ProductionBrand brand
```

### ProductionBrand
```
ProductionBrand
└── @ManyToOne(optional=false) Company company
```

### CatalogImport
```
CatalogImport
└── @ManyToOne(optional=false) Company company
```

---

## HR Module Entities

### Employee
```
Employee
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) SalaryStructureTemplate salaryTemplate
└── @ManyToOne UserAccount userAccount (optional, for login link)
```

### SalaryStructureTemplate
```
SalaryStructureTemplate
└── @ManyToOne(optional=false) Company company
```

### PayrollRun
```
PayrollRun
├── @ManyToOne(optional=false) Company company
├── @ManyToOne(optional=false) AccountingPeriod period
├── @ManyToOne JournalEntry journalEntry (optional, after posting)
└── @OneToMany PayrollRunLine[] lines
```

### PayrollRunLine
```
PayrollRunLine
├── @ManyToOne(optional=false) PayrollRun payrollRun
└── @ManyToOne(optional=false) Employee employee
```

### LeaveRequest
```
LeaveRequest
├── @ManyToOne(optional=false) Employee employee
└── @ManyToOne LeaveTypePolicy leaveType (optional)
```

### LeaveBalance
```
LeaveBalance
├── @ManyToOne(optional=false) Employee employee
└── @ManyToOne(optional=false) LeaveTypePolicy leaveType
```

### LeaveTypePolicy
```
LeaveTypePolicy
└── @ManyToOne(optional=false) Company company
```

### Attendance
```
Attendance
├── @ManyToOne(optional=false) Company company
└── @ManyToOne(optional=false) Employee employee
```

---

## Auth Module Entities

### UserAccount
```
UserAccount
├── @ManyToMany Role[] roles (EAGER)
├── @ManyToMany Permission[] permissions (EAGER)
└── (linked to Company via context, not FK)
```

### Role
```
Role
└── @ManyToMany Permission[] permissions (EAGER)
```

### Permission
```
Permission
└── (standalone, referenced by roles)
```

### MfaRecoveryCode
```
MfaRecoveryCode
└── @ManyToOne UserAccount user
```

### UserPasswordHistory
```
UserPasswordHistory
└── @ManyToOne(optional=false) UserAccount user
```

### PasswordResetToken
```
PasswordResetToken
└── @ManyToOne(optional=false) UserAccount user
```

### RefreshToken
```
RefreshToken
└── (linked to user by username, not FK)
```

### BlacklistedToken
```
BlacklistedToken
└── (standalone, for revoked JWTs)
```

### UserTokenRevocation
```
UserTokenRevocation
└── (standalone, for user-level revocation)
```

---

## Admin Module Entities

### SupportTicket
```
SupportTicket
└── @ManyToOne(optional=false) UserAccount createdBy
```

### ExportRequest
```
ExportRequest
└── @ManyToOne(optional=false) UserAccount requestedBy
```

### ChangelogEntry
```
ChangelogEntry
└── (standalone, system-wide)
```

---

## Company Module Entities

### Company
```
Company
├── @ManyToOne Account receivableAccount (optional)
├── @ManyToOne Account payableAccount (optional)
└── (root tenant entity)
```

### CoATemplate
```
CoATemplate
└── (standalone, reusable chart of accounts)
```

### TenantSupportWarning
```
TenantSupportWarning
└── @ManyToOne(optional=false) Company company
```

### TenantAdminEmailChangeRequest
```
TenantAdminEmailChangeRequest
└── (linked to company via context)
```

---

## Orchestrator Module Entities

### OutboxEvent
```
OutboxEvent
└── (standalone, event outbox pattern)
```

### OrchestratorCommand
```
OrchestratorCommand
└── (standalone, command tracking)
```

### AuditRecord
```
AuditRecord
└── (standalone, orchestrator audit)
```

### OrderAutoApprovalState
```
OrderAutoApprovalState
└── (standalone, auto-approval tracking)
```

### ScheduledJobDefinition
```
ScheduledJobDefinition
└── (standalone, job registry)
```

---

## Core Module Entities

### AuditLog
```
AuditLog
└── (standalone, general audit log)
```

### NumberSequence
```
NumberSequence
└── @ManyToOne(optional=false) Company company
```

### SystemSetting
```
SystemSetting
└── (standalone, key-value settings)
```

---

## Key Foreign Key Relationships Summary

### Financial Chain
```
Company → Account (default receivable/payable)
    │
    ├── Dealer → Account (receivable)
    │       │
    │       ├── SalesOrder → Dealer
    │       │       │
    │       │       └── SalesOrderItem → SalesOrder
    │       │
    │       └── Invoice → Dealer, SalesOrder, JournalEntry
    │               │
    │               └── InvoiceLine → Invoice
    │
    └── Supplier → Account (payable)
            │
            ├── PurchaseOrder → Supplier
            │       │
            │       └── PurchaseOrderLine → PurchaseOrder, RawMaterial
            │
            ├── GoodsReceipt → Supplier, PurchaseOrder
            │       │
            │       └── GoodsReceiptLine → GoodsReceipt, RawMaterial
            │
            └── RawMaterialPurchase → Supplier, JournalEntry
                    │
                    └── RawMaterialPurchaseLine → RawMaterialPurchase, RawMaterial
```

### Inventory Chain
```
Company → ProductionProduct (SKU)
    │
    ├── FinishedGood → ProductionProduct
    │       │
    │       └── FinishedGoodBatch → FinishedGood, JournalEntry
    │               │
    │               ├── PackagingSlip → FinishedGoodBatch
    │               │       │
    │               │       └── PackagingSlipLine → PackagingSlip, FinishedGoodBatch
    │               │
    │               └── InventoryReservation → FinishedGoodBatch, SalesOrder, PackagingSlip
    │
    └── RawMaterial
            │
            └── RawMaterialBatch → RawMaterial, JournalEntry
```

### Production Chain
```
Company → ProductionPlan
    │
    └── ProductionBatch → FinishedGoodBatch
            │
            └── ProductionLog → ProductionBatch, FinishedGoodBatch
                    │
                    ├── ProductionLogMaterial → ProductionLog, RawMaterial
                    │
                    └── PackingRecord → ProductionLog, SourceBatch, TargetBatch, JournalEntry
```

### HR Chain
```
Company → SalaryStructureTemplate
    │
    └── Employee → SalaryStructureTemplate
            │
            ├── LeaveBalance → Employee, LeaveTypePolicy
            │
            ├── LeaveRequest → Employee, LeaveTypePolicy
            │
            ├── Attendance → Employee
            │
            └── PayrollRunLine → PayrollRun, Employee
                    │
                    └── PayrollRun → Company, AccountingPeriod, JournalEntry
```

### Accounting Chain
```
Company → AccountingPeriod
    │       │
    │       ├── AccountingPeriodSnapshot → AccountingPeriod
    │       │
    │       └── JournalEntry → AccountingPeriod
    │               │
    │               ├── JournalLine → JournalEntry, Account
    │               │
    │               ├── DealerLedgerEntry → Dealer, JournalEntry
    │               │
    │               ├── SupplierLedgerEntry → Supplier, JournalEntry
    │               │
    │               └── PartnerSettlementAllocation → JournalEntry, Invoice, Purchase
    │
    └── Account → Account (parent, hierarchy)
```

---

## Entity Count by Module

| Module | Entities | Key Relationships |
|--------|----------|-------------------|
| Accounting | 17 | Journal → Lines, Ledgers, Periods, Settlements |
| Sales | 8 | Dealer → Orders, Credit Requests |
| Purchasing | 7 | Supplier → POs, GRNs, Purchases |
| Inventory | 14 | Products → Batches, Movements, Reservations |
| Factory | 9 | Plans → Logs → Materials, Packing |
| HR | 8 | Employees → Payroll, Leave, Attendance |
| Invoice | 3 | Invoice → Lines, Sequences |
| Production | 3 | Products, Brands, Imports |
| Auth | 8 | Users → Roles → Permissions |
| Admin | 3 | Tickets, Exports, Changelog |
| Company | 4 | Company, Templates, Warnings |
| Orchestrator | 5 | Commands, Events, Jobs |
| Core | 3 | Audit, Sequences, Settings |
| **Total** | **~82** | |
