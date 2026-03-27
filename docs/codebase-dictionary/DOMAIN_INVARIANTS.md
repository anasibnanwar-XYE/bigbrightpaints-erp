# Domain Invariants - BigBright ERP

This document catalogs the business invariants protected by each domain module. Invariants are business rules that must always be true regardless of the operation performed.

---

## Order-to-Cash (O2C) Invariants

### Credit Management

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Credit limit not exceeded | `SalesCoreEngine.createOrder()`, `SalesFulfillmentService` | Order total + current exposure ≤ dealer credit limit |
| Credit exposure includes pending orders | `SalesOrderCreditExposurePolicy` | Pending credit exposure statuses: BOOKED, RESERVED, PENDING_PRODUCTION, PENDING_INVENTORY, PROCESSING, READY_TO_SHIP, CONFIRMED, ON_HOLD |
| Credit check required before confirmation | `SalesOrderLifecycleService.confirmOrder()` | Final credit check even if passed at creation |
| Override requires approval | `CreditLimitOverrideService` | One-time overrides need maker-checker approval |
| Override has expiration | `CreditLimitOverrideService` | Overrides expire after configured time (default 24h) |
| Override is single-use | `CreditLimitOverrideService` | Once used, override is consumed |

### Order Processing

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Order number unique per company | `OrderNumberService` | Sequential numbering within company/fiscal year |
| Order has positive total | `SalesOrderLifecycleService.confirmOrder()` | Cannot confirm zero or negative orders |
| Order belongs to company | `CompanyEntityLookup`, repositories | All queries filtered by company context |
| Status transitions are valid | Lifecycle services | Only allowed transitions permitted |

### Reservation

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Cannot reserve more than available | `SalesFulfillmentService.reserveForOrder()` | Available = currentStock - reservedStock |
| Reservation creates batch link | `SalesFulfillmentService` | Each reservation linked to specific batches |
| FIFO batch selection | `FinishedGoodsService` | Oldest batches reserved first (configurable) |

### Dispatch

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Shipped quantity ≤ ordered quantity | `DispatchMetadataValidator` | Backorders created for shortfall |
| Dispatch requires reservation | `SalesFulfillmentService.dispatchOrder()` | Stock must be reserved before dispatch |
| Dispatch updates both stocks | `FinishedGoodsService` | currentStock and reservedStock both reduced |

### Posting

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Journal entry must balance | `JournalEntryService` | Total debit = Total credit |
| AR journal posted on dispatch | `SalesCoreEngine.confirmDispatch()` | Debit: Dealer Receivable, Credit: Revenue + Tax |
| COGS journal posted on dispatch | `SalesCoreEngine.confirmDispatch()` | Debit: COGS, Credit: Inventory |
| Journal lines use batch costs | `SalesFulfillmentService` | Actual batch cost used, not standard cost |
| Period must be open | `AccountingPeriodService` | Cannot post to closed period |

---

## Procure-to-Pay (P2P) Invariants

### Supplier Management

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Supplier must be ACTIVE for transactions | `PurchasingService` | PENDING/APPROVED/SUSPENDED blocked |
| GSTIN format valid | `SupplierService` | Pattern: `^[0-9]{2}[A-Z0-9]{13}$` |
| Payable account auto-created | `SupplierService` | `AP-{code}` account created with supplier |
| Bank details encrypted | `CryptoService` | AES-256-GCM encryption |

### Purchase Order

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| PO number unique per company | `PurchaseOrderService` | Case-insensitive uniqueness |
| No duplicate materials in PO | `PurchaseOrderService` | Each material appears once per PO |
| Quantities positive | `PurchaseOrderService` | All line quantities > 0 |
| Costs positive | `PurchaseOrderService` | All line costs > 0 |

### Goods Receipt

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Idempotency key required | `GoodsReceiptService` | Prevents duplicate receipts |
| PO must be APPROVED/PARTIALLY_RECEIVED | `GoodsReceiptService` | Cannot receive against DRAFT PO |
| Receipt quantity ≤ remaining ordered | `GoodsReceiptService` | Cannot over-receive |
| Unit must match PO line | `GoodsReceiptService` | Unit consistency enforced |
| Period must be open | `AccountingPeriodService` | Receipt date within open period |

### Invoice

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Invoice lines match goods receipt | `PurchaseInvoiceService` | Same materials, quantities, units, costs |
| Tax mode consistent | `PurchaseInvoiceService` | Cannot mix GST and non-GST in one invoice |
| GST split by state | `GstService` | Intra-state: CGST+SGST, Inter-state: IGST |
| Journal posted first | `PurchaseInvoiceService` | Fail-fast: journal before entity |
| Goods receipt not already invoiced | `PurchaseInvoiceService` | One invoice per receipt |

### Purchase Return

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Purchase must be posted | `PurchaseReturnService` | Cannot return uninvoiced goods |
| Return quantity ≤ remaining | `PurchaseReturnService` | Cannot return more than outstanding |
| Tax credit reversed proportionally | `PurchaseReturnService` | Proportional reversal of input credit |
| Stock deducted from batches | `PurchaseReturnService` | FIFO batch selection for deduction |

### Approval

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| DRAFT → APPROVED requires approval | `PurchaseOrderService.approvePurchaseOrder()` | Explicit approval action |
| APPROVED can be voided | `PurchaseOrderService.voidPurchaseOrder()` | Void with reason required |

---

## Manufacturing-to-Stock (M2S) Invariants

### Production

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Production log creates batch | `FinishedGoodsService` | Each log creates FinishedGoodBatch |
| Batch receives stock | `FinishedGoodsService` | RECEIPT movement recorded |
| Batch cost calculated | `CostAllocationService` | Material + overhead costs |

### Inventory

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Stock never negative | Various services | Validation before deduction |
| Batch quantity consistent | Batch services | Total = sum of movements |
| Costing method enforced | `CostingMethodUtils` | FIFO/LIFO/WAC per product |

### Costing

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Valid costing method | `CostingMethodUtils` | Only FIFO, LIFO, WAC accepted |
| LIFO for finished goods only | `CostingMethodUtils` | Raw materials: FIFO or WAC |
| Default to FIFO | `CostingMethodUtils` | Missing method defaults to FIFO |

### Inventory Movement

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Movement linked to reference | `InventoryMovement` | SALES_ORDER, PRODUCTION_LOG, etc. |
| Movement recorded atomically | Services with @Transactional | Stock update + movement together |
| Movement tracks cost | `InventoryMovement` | unitCost and totalCost captured |

---

## Payroll Invariants

### Employee

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Required fields | `EmployeeService` | firstName, lastName, email required |
| PAN format valid | `EmployeeService` | Indian PAN format validation |
| Date chronology | `EmployeeService` | DOB < joining date |
| Compensation by type | `EmployeeService` | Staff: salary, Labour: wage |
| Bank details encrypted | `CryptoService` | Account details encrypted |

### Attendance

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Attendance within period | `AttendanceService` | Date within payroll period |
| One record per employee per day | `AttendanceService` | No duplicate attendance |
| Hours ≤ working day max | `AttendanceService` | Overtime tracked separately |

### Leave

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Date range valid | `LeaveService` | Start ≤ End |
| No overlapping requests | `LeaveService` | Check for conflicts |
| Balance check | `LeaveService` | If APPROVED, balance deducted |

### Payroll Run

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Status progression | `PayrollRunService` | DRAFT → CALCULATED → APPROVED → POSTED → PAID |
| Cannot skip statuses | Services | Must progress in order |
| Period dates valid | `PayrollRunService` | Start ≤ End |
| Lines exist before approval | `PayrollPostingService` | Cannot approve empty run |
| Gross > 0 before posting | `PayrollPostingService` | Cannot post zero payroll |

### Statutory Deductions

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| PF based on basic | `PayrollCalculationService` | Rate × basic (default 12%) |
| ESI threshold | `PayrollCalculationService` | Only if gross ≤ ₹21,000 |
| ESI rate | `PayrollCalculationService` | 0.5% of gross |
| TDS annual projection | `PayrollCalculationService` | Weekly ×52, Monthly ×12 |
| TDS exemption | `PayrollCalculationService` | OLD: ₹2.5L, NEW: ₹3L |
| TDS rate | `PayrollCalculationService` | 10% of taxable / periods |
| PT fixed amount | `PayrollCalculationService` | Monthly only, default ₹200 |

### Posting

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Journal balances | `PayrollPostingService` | Expense = Payable + Deductions |
| Accounts configured | Company settings | Salary, PF, ESI, TDS, PT accounts exist |
| Attendance linked | `PayrollPostingService` | Attendance records linked to run |
| Advances deducted | `PayrollPostingService` | Employee advances reduced |

---

## Auth Invariants

### Tenant Scoping

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| All queries company-scoped | `CompanyContextFilter`, repositories | WHERE company_id = current |
| Company from token | `CompanyContextFilter` | X-Company-Code must match token |
| User must be company member | `CompanyContextFilter` | Membership validated |
| Super-admin platform-only | `CompanyContextFilter` | Cannot access tenant workflows |

### Tenant Lifecycle

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| ACTIVE allows all | `CompanyContextFilter` | Normal operations |
| SUSPENDED read-only | `CompanyContextFilter` | Only GET/HEAD/OPTIONS |
| DEACTIVATED blocked | `CompanyContextFilter` | All requests denied |
| State transitions valid | `TenantLifecycleService` | Only allowed transitions |

### Password Policy

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Minimum length | `PasswordService` | Configurable minimum |
| Complexity required | `PasswordService` | Mixed case, digits, special chars |
| Not same as current | `PasswordService` | New ≠ current |
| History check | `PasswordService` | Cannot reuse last 5 passwords |
| mustChangePassword enforced | `MustChangePasswordCorridorFilter` | Limited access until changed |

### Session Management

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Token expiration | `JwtTokenService` | Access: 15 min, Refresh: 30 days |
| Single-use refresh | `RefreshTokenService` | Consumed on use |
| Blacklist on logout | `TokenBlacklistService` | Token ID blacklisted |
| Revoke all sessions on password change | `TokenBlacklistService` | All user tokens revoked |

### Account Lockout

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Max failed attempts | `SecurityMonitoringService` | Default: 5 |
| Lockout duration | `SecurityMonitoringService` | Default: 15 minutes |
| Reset on success | `AuthService` | Attempts cleared on login |

### MFA

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Secret encrypted | `MfaService` | AES encryption for stored secret |
| TOTP required if enabled | `JwtAuthenticationFilter` | MFA step if user.mfaEnabled |
| Recovery codes single-use | `MfaService` | Marked used after use |
| 8 recovery codes | `MfaService` | Generated on enrollment |

---

## Cross-Cutting Invariants

### Multi-Tenancy

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| All entities have company_id | Entity design | Mandatory field |
| Company context in thread | `CompanyContextHolder` | Available everywhere |
| Async propagates context | `AsyncConfig` | Context copied to async threads |

### Idempotency

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Key normalization | `IdempotencyHeaderUtils` | Consistent format |
| Payload hash verification | Idempotency services | Detect payload mismatch |
| Single result per key | Idempotency services | Same response on retry |

### Audit Trail

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Actor attribution | `SecurityActorResolver` | Never null |
| Timestamp captured | `CompanyTime` | Company timezone |
| Event recorded | `AuditService` | Async write |

### Concurrency

| Invariant | Enforcement Point | Description |
|-----------|-------------------|-------------|
| Optimistic locking | `@Version` on entities | Version column check |
| Pessimistic locking | Repository locks | Critical sections protected |
| Conflict error | `ErrorCode.CONCURRENCY_CONFLICT` | Clear error on conflict |

---

## Summary Table

| Domain | Critical Invariants | Enforcement Points |
|--------|---------------------|-------------------|
| O2C (Credit) | 6 | SalesCoreEngine, CreditLimitOverrideService |
| O2C (Order) | 4 | SalesOrderLifecycleService, OrderNumberService |
| O2C (Reservation) | 3 | SalesFulfillmentService, FinishedGoodsService |
| O2C (Dispatch) | 3 | DispatchMetadataValidator, SalesFulfillmentService |
| O2C (Posting) | 5 | JournalEntryService, AccountingPeriodService |
| P2P (Supplier) | 4 | SupplierService, CryptoService |
| P2P (PO) | 4 | PurchaseOrderService |
| P2P (Receipt) | 5 | GoodsReceiptService |
| P2P (Invoice) | 5 | PurchaseInvoiceService, GstService |
| P2P (Return) | 4 | PurchaseReturnService |
| M2S (Production) | 3 | FinishedGoodsService, CostAllocationService |
| M2S (Inventory) | 3 | Various inventory services |
| M2S (Costing) | 3 | CostingMethodUtils |
| Payroll (Employee) | 5 | EmployeeService |
| Payroll (Run) | 5 | PayrollRunService, PayrollPostingService |
| Payroll (Statutory) | 7 | PayrollCalculationService |
| Auth (Tenant) | 4 | CompanyContextFilter |
| Auth (Password) | 5 | PasswordService |
| Auth (Session) | 4 | JwtTokenService, TokenBlacklistService |
| Auth (Lockout) | 3 | SecurityMonitoringService |
| Auth (MFA) | 4 | MfaService |
