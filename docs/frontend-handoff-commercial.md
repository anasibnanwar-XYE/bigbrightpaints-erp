# Frontend Handoff — Commercial Surfaces

Last reviewed: 2026-03-30

This packet documents the frontend contract for **commercial surfaces** — sales/order-to-cash, purchasing/procure-to-pay, invoices, and dealer finance. It explains canonical hosts, payload families, RBAC assumptions, read/write boundaries, and the internal-vs-self-service parity between admin/portal and dealer views.

This packet defers to the canonical module and flow docs for implementation truth and is not a second source of truth.

---

## 1. Scope Overview

| Surface | Module | Canonical Doc |
| --- | --- | --- |
| Sales/Order-to-Cash | `sales` (SalesController, DealerController) | [docs/modules/sales.md](modules/sales.md) |
| Purchasing/P2P | `purchasing` (SupplierController, PurchasingWorkflowController) | [docs/modules/purchasing.md](modules/purchasing.md) |
| Invoices/Dealer Finance | `invoice` + `sales` (DealerPortalController) | [docs/modules/invoice.md](modules/invoice.md) |

---

## 2. Canonical Host Prefixes

### 2.1 Sales Routes

| Route | Method | Actor | Read/Write |
| --- | --- | :---: | :---: |
| `/api/v1/sales/orders` | GET | ADMIN, SALES, FACTORY, ACCOUNTING | Read |
| `/api/v1/sales/orders` | POST | SALES, ADMIN | Write |
| `/api/v1/sales/orders/{id}` | GET | ADMIN, SALES, FACTORY, ACCOUNTING | Read |
| `/api/v1/sales/orders/{id}` | PUT | SALES, ADMIN | Write |
| `/api/v1/sales/orders/{id}` | DELETE | SALES, ADMIN | Write (draft only) |
| `/api/v1/sales/orders/{id}/confirm` | POST | SALES, ADMIN | Write |
| `/api/v1/sales/orders/{id}/cancel` | POST | SALES, ADMIN | Write |
| `/api/v1/sales/orders/{id}/status` | PATCH | SALES, ADMIN | Write (manual statuses) |
| `/api/v1/sales/orders/{id}/timeline` | GET | ADMIN, SALES, FACTORY, ACCOUNTING | Read |
| `/api/v1/sales/dealers` | GET | ADMIN, SALES, ACCOUNTING | Read |
| `/api/v1/sales/dealers/search` | GET | ADMIN, SALES, ACCOUNTING | Read |
| `/api/v1/sales/dashboard` | GET | ADMIN, SALES, FACTORY, ACCOUNTING | Read |
| `/api/v1/credit/limit-requests` | GET | ADMIN, SALES | Read |
| `/api/v1/credit/limit-requests` | POST | SALES, ADMIN | Write |
| `/api/v1/credit/limit-requests/{id}/approve` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/credit/limit-requests/{id}/reject` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/credit/override-requests` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/credit/override-requests` | POST | ADMIN, FACTORY, SALES | Write |
| `/api/v1/credit/override-requests/{id}/approve` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/credit/override-requests/{id}/reject` | POST | ADMIN, ACCOUNTING | Write |

### 2.2 Dealer Management Routes

| Route | Method | Actor | Read/Write |
| --- | --- | :---: | :---: |
| `/api/v1/dealers` | GET | ADMIN, SALES, ACCOUNTING | Read |
| `/api/v1/dealers` | POST | ADMIN, SALES, ACCOUNTING | Write |
| `/api/v1/dealers/{dealerId}` | GET | ADMIN, SALES, ACCOUNTING | Read |
| `/api/v1/dealers/{dealerId}` | PUT | ADMIN, SALES, ACCOUNTING | Write |
| `/api/v1/dealers/{dealerId}/dunning/hold` | POST | ADMIN, SALES, ACCOUNTING | Write |
| `/api/v1/dealers/search` | GET | ADMIN, SALES, ACCOUNTING | Read |

### 2.3 Purchasing Routes

| Route | Method | Actor | Read/Write |
| --- | --- | :---: | :---: |
| `/api/v1/suppliers` | GET | ADMIN, ACCOUNTING, FACTORY | Read |
| `/api/v1/suppliers/{id}` | GET | ADMIN, ACCOUNTING, FACTORY | Read |
| `/api/v1/suppliers` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/suppliers/{id}` | PUT | ADMIN, ACCOUNTING | Write |
| `/api/v1/suppliers/{id}/approve` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/suppliers/{id}/activate` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/suppliers/{id}/suspend` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/purchase-orders` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/purchasing/purchase-orders` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/purchase-orders/{id}` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/purchasing/purchase-orders/{id}/approve` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/purchase-orders/{id}/void` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/purchase-orders/{id}/close` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/purchase-orders/{id}/timeline` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/purchasing/goods-receipts` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/purchasing/goods-receipts` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/goods-receipts/{id}` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/purchasing/raw-material-purchases` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/purchasing/raw-material-purchases` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/raw-material-purchases/{id}` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/purchasing/raw-material-purchases/returns` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/purchasing/raw-material-purchases/returns/preview` | POST | ADMIN, ACCOUNTING | Write |

### 2.4 Invoice and Dealer Finance Routes

#### Internal Finance (Admin/Accounting)

| Route | Method | Actor | Read/Write |
| --- | --- | :---: | :---: |
| `/api/v1/invoices` | GET | ADMIN, SALES, ACCOUNTING | Read |
| `/api/v1/invoices/{id}` | GET | ADMIN, SALES, ACCOUNTING | Read |
| `/api/v1/invoices/{id}/pdf` | GET | ADMIN | Read |
| `/api/v1/invoices/{id}/email` | POST | ADMIN, SALES, ACCOUNTING | Write |
| `/api/v1/portal/finance/ledger` | GET | ADMIN, ACCOUNTING | Read (requires `dealerId` query) |
| `/api/v1/portal/finance/invoices` | GET | ADMIN, ACCOUNTING | Read (requires `dealerId` query) |
| `/api/v1/portal/finance/aging` | GET | ADMIN, ACCOUNTING | Read (requires `dealerId` query) |
| `/api/v1/accounting/sales/returns` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/accounting/sales/returns` | POST | ADMIN, ACCOUNTING | Write |

#### Dealer Self-Service (ROLE_DEALER only)

| Route | Method | Actor | Read/Write |
| --- | --- | :---: | :---: |
| `/api/v1/dealer-portal/dashboard` | GET | DEALER | Read (auto-scoped) |
| `/api/v1/dealer-portal/ledger` | GET | DEALER | Read (auto-scoped) |
| `/api/v1/dealer-portal/invoices` | GET | DEALER | Read (auto-scoped) |
| `/api/v1/dealer-portal/invoices/{id}/pdf` | GET | DEALER | Read (own invoice only) |
| `/api/v1/dealer-portal/aging` | GET | DEALER | Read (auto-scoped) |
| `/api/v1/dealer-portal/orders` | GET | DEALER | Read (auto-scoped) |
| `/api/v1/dealer-portal/credit-limit-requests` | POST | DEALER | Write (own dealer) |
| `/api/v1/dealer-portal/support/tickets` | GET/POST | DEALER | Read/Write |
| `/api/v1/dealer-portal/support/tickets/{ticketId}` | GET | DEALER | Read |

### 2.5 Settlement Routes (Accounting)

| Route | Method | Actor | Read/Write |
| --- | --- | :---: | :---: |
| `/api/v1/accounting/receipts/dealer` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/accounting/receipts/dealer/hybrid` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/accounting/settlements/dealers` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/accounting/settlements/suppliers` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/accounting/statements/suppliers/{supplierId}` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/accounting/aging/suppliers/{supplierId}` | GET | ADMIN, ACCOUNTING | Read |
| `/api/v1/accounting/credit-notes` | POST | ADMIN, ACCOUNTING | Write |
| `/api/v1/accounting/bad-debts/write-off` | POST | ADMIN, ACCOUNTING | Write |

### 2.6 Dispatch Routes

| Route | Method | Actor | Read/Write |
| --- | --- | :---: | :---: |
| `/api/v1/dispatch/pending` | GET | ADMIN, FACTORY, SALES | Read |
| `/api/v1/dispatch/preview/{slipId}` | GET | ADMIN, FACTORY | Read |
| `/api/v1/dispatch/confirm` | POST | OPERATIONAL_DISPATCH | Write |
| `/api/v1/dispatch/slip/{slipId}` | GET | ADMIN, FACTORY, SALES | Read |
| `/api/v1/dispatch/order/{orderId}` | GET | ADMIN, FACTORY, SALES | Read |
| `/api/v1/dispatch/slip/{slipId}/status` | PATCH | ADMIN, FACTORY | Write |
| `/api/v1/dispatch/slip/{slipId}/challan/pdf` | GET | ADMIN, FACTORY | Read |

---

## 3. RBAC Summary

### 3.1 Role Permissions by Surface

| Role | Sales (Read) | Sales (Write) | Purchasing | Invoices (Admin) | Dealer Finance (Internal) | Dealer Portal |
| --- | :---: | :---: | :---: | :---: | :---: | :---: |
| `ROLE_ADMIN` | Full | Full | Full | Full | Full | — |
| `ROLE_ACCOUNTING` | Full | — | Full | Full | Full | — |
| `ROLE_SALES` | Full | Full | — | Read (list/email) | — | — |
| `ROLE_FACTORY` | Read (orders) | — | Read (suppliers) | — | — | — |
| `ROLE_DEALER` | — | — | — | — | — | Read (self), Write (credit requests) |

### 3.2 Key RBAC Boundaries

1. **Sales order lifecycle** — Order creation and confirmation requires SALES or ADMIN. Accounting can view but not create orders.

2. **Dispatch confirmation** — Requires `OPERATIONAL_DISPATCH` role (or factory role with transport metadata validation).

3. **Invoice PDF** — Admin-only for internal invoice PDF download. Dealers can download their own invoices.

4. **Portal finance (internal)** — Admin and Accounting only. Sales and Factory explicitly blocked.

5. **Dealer portal** — Dealer-only. Auto-scoped to authenticated dealer's data. No dealer ID parameters.

6. **Credit limit requests** — Creation: SALES/ADMIN (internal) or DEALER (self-service). Approval: ADMIN/ACCOUNTING only.

7. **Supplier management** — Admin and Accounting can create/approve/activate/suspend. Factory can read.

---

## 4. Payload Families

### 4.1 Sales Payloads

**Order creation:**
- `SalesOrderRequest` — `dealerId`, `items[]` (productCode, quantity, unitPrice, gstRate), `paymentMode` (CREDIT/SPLIT), `gstTreatment`, `idempotencyKey`, optional `notes`, `referenceNumber`
- Response: `SalesOrderDto` with status, amounts, GST, journal entry ID, invoice ID

**Dealer creation:**
- `CreateDealerRequest` — `name`, `email`, `phone`, `gstNumber`, `creditLimit`, `paymentTerms`, `region`, `address`
- Response: `DealerResponse` with outstanding balance, receivable account, portal email

**Credit requests:**
- `CreditLimitRequestCreateRequest` — `dealerId`, `amountRequested`, `reason`
- `CreditLimitOverrideRequestCreateRequest` — `packagingSlipId` or `salesOrderId`, `overrideAmount`, `reason`

### 4.2 Purchasing Payloads

**Supplier:**
- `SupplierRequest` — `name`, `code`, `paymentTerms`, `gstNumber`, `address`, `contactEmail`, `contactPhone`, `bankDetails`
- Response includes auto-generated payable account (`AP-{CODE}`)

**Purchase order:**
- `PurchaseOrderRequest` — `supplierId`, `orderNumber`, `lineItems[]` (rawMaterialId, quantity, unitRate), `memo`
- No idempotency key — relies on order number uniqueness

**Goods receipt:**
- `GoodsReceiptRequest` — `purchaseOrderId`, `receiptDate`, `receiptNumber`, `lines[]` (rawMaterialId, quantity, costPerUnit, batchCode, notes), `memo`
- Requires `Idempotency-Key` header

**Purchase invoice (raw material purchase):**
- `RawMaterialPurchaseRequest` — `goodsReceiptId`, `invoiceNumber`, `invoiceDate`, `supplierId`, `lines[]` (rawMaterialId, quantity, costPerUnit, taxRate, taxInclusive), `taxAmount`
- Creates AP entries in accounting

### 4.3 Invoice Payloads

**Invoice list/detail:**
- `InvoiceDto` — `invoiceNo`, `invoiceDate`, `dealerName`, `grossAmount`, `taxAmount`, `netAmount`, `status`, `dueDate`

**Invoice PDF/email:**
- PDF download via `/api/v1/invoices/{id}/pdf` (Admin) or `/api/v1/dealer-portal/invoices/{id}/pdf` (Dealer)
- Email via `/api/v1/invoices/{id}/email` — requires recipient email in body

### 4.4 Dealer Finance Payloads (Internal)

- `PortalFinanceLedgerRequest` — `dealerId` (query param)
- `PortalFinanceAgingRequest` — `dealerId` (query param), optional `asOf`, `buckets`
- Response structures: ledger entries with running balance, aging buckets (0-30, 31-60, 61-90, 90+)

### 4.5 Settlement Payloads

**Dealer receipt:**
- `DealerReceiptRequest` — `dealerId`, `amount`, `cashAccountId`, `allocations[]` (invoiceId, appliedAmount, discountAmount, writeOffAmount), `referenceNumber`, `Idempotency-Key`
- Hybrid variant: `DealerHybridReceiptRequest` — multiple incoming lines with account assignment

**Dealer settlement:**
- `DealerSettlementRequest` — `dealerId`, `payments[]` (accountId, amount, method), `allocations[]`, `cashAccountId`, `discountAccountId`, `writeOffAccountId`

---

## 5. Read/Write Boundaries

### 5.1 Sales/Order-to-Cash

| Action | Allowed Roles | Notes |
| --- | :---: | --- |
| Create order | SALES, ADMIN | Idempotent via header or body key |
| Confirm order | SALES, ADMIN | Triggers credit check + stock validation |
| Cancel order | SALES, ADMIN | Requires reason code |
| Manual status | SALES, ADMIN | ON_HOLD, REJECTED, CLOSED only |
| View orders | ADMIN, SALES, FACTORY, ACCOUNTING | — |
| Credit limit requests (create) | SALES, ADMIN, DEALER | DEALER creates for self |
| Credit limit requests (approve) | ADMIN, ACCOUNTING | — |
| Credit override (create) | ADMIN, FACTORY, SALES | Per-dispatch overrides |
| Credit override (approve) | ADMIN, ACCOUNTING | — |

### 5.2 Purchasing/Procure-to-Pay

| Action | Allowed Roles | Notes |
| --- | :---: | --- |
| Create supplier | ADMIN, ACCOUNTING | Starts in PENDING status |
| Approve supplier | ADMIN, ACCOUNTING | PENDING → APPROVED |
| Activate supplier | ADMIN, ACCOUNTING | APPROVED → ACTIVE |
| Suspend supplier | ADMIN, ACCOUNTING | ACTIVE → SUSPENDED |
| Create PO | ADMIN, ACCOUNTING | DRAFT status |
| Approve PO | ADMIN, ACCOUNTING | DRAFT → APPROVED |
| Void PO | ADMIN, ACCOUNTING | DRAFT/APPROVED only |
| Create GRN | ADMIN, ACCOUNTING | Requires Idempotency-Key |
| Create purchase invoice | ADMIN, ACCOUNTING | Links to GRN |
| Purchase return | ADMIN, ACCOUNTING | Preview then create |
| Supplier settlement | ADMIN, ACCOUNTING | Manual or auto-settle |

### 5.3 Invoices

| Action | Allowed Roles | Notes |
| --- | :---: | --- |
| List invoices | ADMIN, SALES, ACCOUNTING | — |
| View invoice | ADMIN, SALES, ACCOUNTING | — |
| Download PDF (internal) | ADMIN only | — |
| Email invoice | ADMIN, SALES, ACCOUNTING | — |
| Credit notes | ADMIN, ACCOUNTING | Against invoice |
| Bad debt write-off | ADMIN, ACCOUNTING | Against invoice |
| Sales returns | ADMIN, ACCOUNTING | Goods return + reversal |

### 5.4 Dealer Finance — Internal vs Self-Service Parity

| Surface | Internal (Portal Finance) | Dealer Self-Service |
| --- | :--- | :--- |
| Ledger | `/api/v1/portal/finance/ledger?dealerId={id}` (ADMIN/ACCOUNTING) | `/api/v1/dealer-portal/ledger` (DEALER, auto-scoped) |
| Invoices | `/api/v1/portal/finance/invoices?dealerId={id}` (ADMIN/ACCOUNTING) | `/api/v1/dealer-portal/invoices` (DEALER, auto-scoped) |
| Aging | `/api/v1/portal/finance/aging?dealerId={id}` (ADMIN/ACCOUNTING) | `/api/v1/dealer-portal/aging` (DEALER, auto-scoped) |
| Dashboard | — | `/api/v1/dealer-portal/dashboard` (DEALER, auto-scoped) |
| Credit requests | `/api/v1/credit/limit-requests` (ADMIN/ACCOUNTING approve) | `/api/v1/dealer-portal/credit-limit-requests` (DEALER create) |
| Orders | `/api/v1/sales/orders` (ADMIN/SALES view any) | `/api/v1/dealer-portal/orders` (DEALER view own) |

**Key parity notes:**
- Internal routes use `dealerId` query param; dealer routes auto-scope
- Dealer routes require `ROLE_DEALER`; internal require `ROLE_ADMIN` or `ROLE_ACCOUNTING`
- Invoice PDF access: internal is ADMIN-only; dealer is own-invoice only
- Sales roles are blocked from portal finance routes

---

## 6. Host/Path Ownership Summary

| Surface | Host Family | Ownership |
| --- | :--- | :--- |
| Sales orders | `/api/v1/sales/**` | Sales module |
| Dealer management | `/api/v1/dealers/**` | Sales module |
| Credit requests | `/api/v1/credit/**` | Sales module |
| Purchasing | `/api/v1/purchasing/**` + `/api/v1/suppliers/**` | Purchasing module |
| Invoices | `/api/v1/invoices/**` | Invoice module |
| Portal finance (internal) | `/api/v1/portal/finance/**` | Portal module |
| Dealer portal | `/api/v1/dealer-portal/**` | Sales module |
| Settlements | `/api/v1/accounting/settlements/**` | Accounting module |
| Dispatch | `/api/v1/dispatch/**` | Inventory (controller), Sales (commercial) |

---

## 7. Cross-Module Seams for Frontend Awareness

### 7.1 Sales → Inventory Handoff

- Order creation triggers stock reservation via `FinishedGoodsReservationEngine`
- Dispatch confirmation triggers inventory dispatch + COGS posting + invoice issuance
- Dispatch controller is in inventory module but commercial ownership is in sales module

### 7.2 Sales → Accounting Handoff

- Dispatch confirmation posts AR/revenue journal (dealer receivable → revenue + GST)
- COGS journal posts on dispatch (inventory → COGS)
- Invoice is auto-created during dispatch if `issueInvoice=true`
- Sales returns trigger reversal journals + credit notes

### 7.3 Purchasing → Inventory Handoff

- GRN creates raw material batches in inventory module
- Raw material batches increase stock and create RECEIPT movements
- Stock truth lives in inventory; AP truth lives in accounting

### 7.4 Purchasing → Accounting Handoff

- Purchase invoice creates AP entries (supplier payable account)
- Settlement reduces AP balance and posts to bank/cash
- Two distinct truths: stock (inventory) and AP (accounting)

### 7.5 Invoice → Accounting Handoff

- Invoice creation triggers AR/revenue journal via internal AccountingService
- Settlement handled by accounting module (not via AccountingFacade)
- Invoice status mutable; no automated reconciliation

---

## 8. Known Safety Gaps for Frontend

| Gap | Description | Mitigation |
| --- | :--- | :--- |
| Credit limit check on order creation | Credit check runs on creation for CREDIT-mode orders; may fail after frontend submits | Frontend should pre-check dealer's credit status |
| Credit override link required | Override request must link to specific slip/order | Frontend must pass slip/order ID |
| PO has no idempotency | Order number uniqueness is the only duplicate prevention | Frontend should generate unique order numbers |
| Invoice creation not exposed | Invoice is auto-created during dispatch; no separate create API | Frontend goes through dispatch to create invoice |
| Settlement is manual | No automatic payment reconciliation | Frontend implements manual settlement UI |
| Order status not automatically closed | Must manually close via PATCH after settlement | Frontend adds close step in settlement flow |
| No partial fulfillment by default | Orders with shortages fail unless explicitly allowed | Frontend handles shortage display |

---

## 9. Deprecation Notes

| Surface | Status | Replacement |
| --- | :--- | :--- |
| `/api/v1/sales/dealers` aliases | Alias only | Use `/api/v1/dealers` as canonical |
| Legacy payment mode idempotency | Supported for backward compat | Canonical header or body key |
| BOOKED, SHIPPED, FULFILLED, COMPLETED statuses | Legacy | Use canonical statuses |
| X-Idempotency-Key (sales) | Legacy alias | Use canonical Idempotency-Key |

---

## Cross-References

- [docs/INDEX.md](INDEX.md) — canonical documentation index
- [docs/modules/sales.md](modules/sales.md) — sales module doc
- [docs/modules/purchasing.md](modules/purchasing.md) — purchasing module doc
- [docs/modules/invoice.md](modules/invoice.md) — invoice module doc
- [docs/modules/inventory.md](modules/inventory.md) — inventory (dispatch) doc
- [docs/modules/admin-portal-rbac.md](modules/admin-portal-rbac.md) — RBAC and host ownership
- [docs/flows/order-to-cash.md](flows/order-to-cash.md) — O2C flow
- [docs/flows/procure-to-pay.md](flows/procure-to-pay.md) — P2P flow
- [docs/flows/invoice-dealer-finance.md](flows/invoice-dealer-finance.md) — invoice flow
- [docs/accounting-portal-frontend-engineer-handoff.md](accounting-portal-frontend-engineer-handoff.md) — accounting portal handoff
- [docs/frontend-handoff-operations.md](frontend-handoff-operations.md) — operations handoff
