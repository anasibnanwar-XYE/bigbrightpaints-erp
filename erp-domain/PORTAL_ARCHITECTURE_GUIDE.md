# BigBright ERP - Portal Architecture Guide

**For Frontend Developers**

---

## Portal Structure Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        BigBright ERP System                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│   │   ADMIN     │  │ ACCOUNTING  │  │   SALES     │  │  FACTORY    │       │
│   │   PORTAL    │  │   PORTAL    │  │   PORTAL    │  │   PORTAL    │       │
│   ├─────────────┤  ├─────────────┤  ├─────────────┤  ├─────────────┤       │
│   │ • Users     │  │ • Accounts  │  │ • Dealers   │  │ • Mixing    │       │
│   │ • Roles     │  │ • Journals  │  │ • Orders    │  │ • Packing   │       │
│   │ • Companies │  │ • Reports   │  │ • Targets   │  │ • Batches   │       │
│   │ • HR/Payroll│  │ • Purchase  │  │ • Promos    │  │ • Dispatch  │       │
│   │ • Settings  │  │ • Invoices  │  │ • Returns   │  │ • QC        │       │
│   └─────────────┘  │ • Payments  │  └─────────────┘  └─────────────┘       │
│                    │ • GST       │                                          │
│                    └─────────────┘                                          │
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │                      DEALER PORTAL                               │       │
│   │  (External - Dealers login to view their orders/invoices/ledger) │       │
│   └─────────────────────────────────────────────────────────────────┘       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. ADMIN PORTAL

**Purpose:** System administration, user management, company settings, HR & Payroll

**URL Pattern:** `/admin/*`

**Roles Required:** `ROLE_ADMIN`

### Modules

#### 1.1 User Management
```http
GET    /api/v1/auth/users              # List all users
POST   /api/v1/auth/users              # Create user (sends credentials email)
GET    /api/v1/auth/users/{id}         # Get user details
PUT    /api/v1/auth/users/{id}         # Update user
DELETE /api/v1/auth/users/{id}         # Deactivate user
POST   /api/v1/auth/users/{id}/reset-password  # Force password reset
```

#### 1.2 Role Management
```http
GET    /api/v1/rbac/roles              # List roles
POST   /api/v1/rbac/roles              # Create role
PUT    /api/v1/rbac/roles/{id}         # Update role permissions
GET    /api/v1/rbac/permissions        # List all permissions
```

#### 1.3 Company Management
```http
GET    /api/v1/companies               # List companies
POST   /api/v1/companies               # Create company
GET    /api/v1/companies/{id}          # Get company details
PUT    /api/v1/companies/{id}          # Update company
```

#### 1.4 HR Module (Inside Admin)
```http
# Employees
GET    /api/v1/hr/employees            # List employees
POST   /api/v1/hr/employees            # Create employee
GET    /api/v1/hr/employees/{id}       # Get employee
PUT    /api/v1/hr/employees/{id}       # Update employee

# Payroll (Links to Accounting)
POST   /api/v1/hr/payroll/process      # Run payroll → Creates journal entries
GET    /api/v1/hr/payroll/slips/{empId}# Get pay slips
GET    /api/v1/hr/payroll/summary      # Payroll summary report
```

#### 1.5 System Settings
```http
GET    /api/v1/settings                # Get all settings
PUT    /api/v1/settings                # Update settings
GET    /api/v1/settings/accounting     # Accounting-specific settings
```

### Admin Portal Pages

| Page | Route | Description |
|------|-------|-------------|
| Dashboard | `/admin` | System overview, user stats |
| Users | `/admin/users` | User CRUD |
| Roles | `/admin/roles` | Role & permission management |
| Companies | `/admin/companies` | Multi-company setup |
| Employees | `/admin/hr/employees` | Employee management |
| Payroll | `/admin/hr/payroll` | Run payroll, view slips |
| Settings | `/admin/settings` | System configuration |

---

## 2. ACCOUNTING PORTAL

**Purpose:** Chart of accounts, journal entries, financial reports, purchase management, invoice management, payments

**URL Pattern:** `/accounting/*`

**Roles Required:** `ROLE_ADMIN`, `ROLE_ACCOUNTING`

### Modules

#### 2.1 Chart of Accounts
```http
GET    /api/v1/accounting/accounts           # List all accounts
POST   /api/v1/accounting/accounts           # Create account
GET    /api/v1/accounting/accounts/{id}      # Get account details
PUT    /api/v1/accounting/accounts/{id}      # Update account
DELETE /api/v1/accounting/accounts/{id}      # Deactivate account
GET    /api/v1/accounting/accounts/tree      # Hierarchical view
```

**Account Types:**
- `ASSET` - Cash, Bank, Inventory, Receivables
- `LIABILITY` - Payables, Loans, Tax Payable
- `EQUITY` - Capital, Retained Earnings
- `REVENUE` - Sales, Service Income
- `EXPENSE` - COGS, Salaries, Utilities

#### 2.2 Journal Entries
```http
GET    /api/v1/accounting/journal-entries           # List entries
POST   /api/v1/accounting/journal-entries           # Create entry
GET    /api/v1/accounting/journal-entries/{id}      # Get entry
POST   /api/v1/accounting/journal-entries/{id}/reverse  # Reverse entry
GET    /api/v1/accounting/journal-entries/{id}/lines    # Get lines
```

**Journal Entry Request:**
```json
{
  "referenceNumber": "JE-2024-001",
  "entryDate": "2024-01-15",
  "memo": "Office supplies",
  "dealerId": null,
  "supplierId": null,
  "autoApprove": true,
  "lines": [
    {"accountId": 1, "description": "Expense", "debit": 1000, "credit": 0},
    {"accountId": 2, "description": "Cash", "debit": 0, "credit": 1000}
  ]
}
```

#### 2.3 Accounting Periods
```http
GET    /api/v1/accounting/periods              # List periods
POST   /api/v1/accounting/periods              # Create period
POST   /api/v1/accounting/periods/{id}/close   # Close period (no more entries)
POST   /api/v1/accounting/periods/{id}/lock    # Lock period
POST   /api/v1/accounting/periods/{id}/reopen  # Reopen (admin only)
```

#### 2.4 Financial Reports
```http
GET    /api/v1/accounting/trial-balance?asOf=2024-01-31
GET    /api/v1/accounting/profit-loss?from=2024-01-01&to=2024-01-31
GET    /api/v1/accounting/balance-sheet?asOf=2024-01-31
GET    /api/v1/accounting/ledger/{accountId}?from=2024-01-01&to=2024-01-31
GET    /api/v1/accounting/cash-flow?from=2024-01-01&to=2024-01-31
```

#### 2.5 Purchase Management (Inside Accounting)
```http
# Suppliers
GET    /api/v1/suppliers                       # List suppliers
POST   /api/v1/suppliers                       # Create supplier
GET    /api/v1/suppliers/{id}                  # Get supplier
PUT    /api/v1/suppliers/{id}                  # Update supplier

# Purchase Orders
GET    /api/v1/purchasing/orders               # List POs
POST   /api/v1/purchasing/orders               # Create PO
GET    /api/v1/purchasing/orders/{id}          # Get PO details
PUT    /api/v1/purchasing/orders/{id}/approve  # Approve PO

# Goods Receipt (GRN)
POST   /api/v1/purchasing/receipts             # Record goods received
GET    /api/v1/purchasing/receipts             # List GRNs

# Purchase Returns
POST   /api/v1/purchasing/returns              # Create return
```

**Purchase Order Request:**
```json
{
  "supplierId": 1,
  "orderDate": "2024-01-15",
  "expectedDelivery": "2024-01-20",
  "items": [
    {"rawMaterialId": 1, "quantity": 100, "unitPrice": 50.00}
  ],
  "notes": "Urgent order"
}
```

#### 2.6 Invoice Management (Inside Accounting)
```http
GET    /api/v1/invoices                        # List all invoices
GET    /api/v1/invoices/{id}                   # Get invoice details
GET    /api/v1/invoices/{id}/pdf               # Download PDF
POST   /api/v1/invoices/{id}/email             # Send invoice email
GET    /api/v1/invoices/dealers/{dealerId}     # Invoices by dealer
GET    /api/v1/invoices/outstanding            # Unpaid invoices
```

**Note:** Invoices are auto-generated on dispatch. Manual invoice creation is not typical.

#### 2.7 Payments & Settlements
```http
# Receive Payment (from Dealer)
POST   /api/v1/accounting/settlements/dealer
{
  "dealerId": 1,
  "amount": 5000.00,
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "UTR123456",
  "invoiceIds": [101, 102]
}

# Make Payment (to Supplier)
POST   /api/v1/accounting/settlements/supplier
{
  "supplierId": 1,
  "amount": 10000.00,
  "paymentMethod": "CHEQUE",
  "chequeNumber": "CH001"
}

# Settlement List
GET    /api/v1/accounting/settlements?type=DEALER
GET    /api/v1/accounting/settlements?type=SUPPLIER
```

#### 2.8 GST Reports
```http
GET    /api/v1/accounting/gst/summary?period=2024-01
GET    /api/v1/accounting/gst/input-credit
GET    /api/v1/accounting/gst/output-liability
```

### Accounting Portal Pages

| Page | Route | Description |
|------|-------|-------------|
| Dashboard | `/accounting` | Financial overview |
| Accounts | `/accounting/accounts` | Chart of accounts |
| Journal Entries | `/accounting/journals` | Create/view entries |
| Trial Balance | `/accounting/reports/trial-balance` | Trial balance report |
| P&L | `/accounting/reports/profit-loss` | Income statement |
| Balance Sheet | `/accounting/reports/balance-sheet` | Balance sheet |
| Suppliers | `/accounting/suppliers` | Supplier management |
| Purchase Orders | `/accounting/purchases` | PO management |
| Invoices | `/accounting/invoices` | Invoice list & email |
| Payments | `/accounting/payments` | Settlements |
| GST | `/accounting/gst` | GST reports |

---

## 3. SALES PORTAL

**Purpose:** Dealer management, sales orders, targets, promotions, returns

**URL Pattern:** `/sales/*`

**Roles Required:** `ROLE_ADMIN`, `ROLE_SALES`

### Modules

#### 3.1 Dealer Management
```http
GET    /api/v1/dealers                         # List dealers
POST   /api/v1/dealers                         # Create dealer
GET    /api/v1/dealers/{id}                    # Get dealer
PUT    /api/v1/dealers/{id}                    # Update dealer
GET    /api/v1/dealers/{id}/ledger             # Dealer ledger
GET    /api/v1/dealers/{id}/statement          # Account statement
```

**Create Dealer:**
```json
{
  "name": "ABC Paints Shop",
  "email": "abc@example.com",
  "phone": "+91-9876543210",
  "gstin": "27AABCU9603R1ZM",
  "pan": "AABCU9603R",
  "address": "123 Market Street, Mumbai",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "creditLimit": 100000.00,
  "paymentTermDays": 30
}
```

#### 3.2 Sales Orders
```http
GET    /api/v1/sales/orders                    # List orders
POST   /api/v1/sales/orders                    # Create order
GET    /api/v1/sales/orders/{id}               # Get order details
PUT    /api/v1/sales/orders/{id}               # Update order
POST   /api/v1/sales/orders/{id}/confirm       # Confirm order
POST   /api/v1/sales/orders/{id}/cancel        # Cancel order
GET    /api/v1/sales/orders/{id}/status        # Track status
```

**Create Sales Order:**
```json
{
  "dealerId": 1,
  "orderDate": "2024-01-15",
  "deliveryDate": "2024-01-20",
  "shippingAddress": "123 Market Street",
  "items": [
    {
      "productId": 1,
      "quantity": 10,
      "unitPrice": 500.00,
      "taxRate": 18.00,
      "discount": 0
    }
  ],
  "notes": "Handle with care"
}
```

**Order Status Flow:**
```
DRAFT → CONFIRMED → RESERVED → PROCESSING → PACKED → DISPATCHED → DELIVERED
                  ↓
            PENDING_PRODUCTION (if no stock)
```

#### 3.3 Sales Targets
```http
GET    /api/v1/sales/targets                   # List targets
POST   /api/v1/sales/targets                   # Create target
PUT    /api/v1/sales/targets/{id}              # Update target
GET    /api/v1/sales/targets/performance       # Performance vs target
```

#### 3.4 Promotions
```http
GET    /api/v1/sales/promotions                # List promotions
POST   /api/v1/sales/promotions                # Create promotion
PUT    /api/v1/sales/promotions/{id}           # Update promotion
DELETE /api/v1/sales/promotions/{id}           # End promotion
```

#### 3.5 Sales Returns
```http
POST   /api/v1/sales/returns                   # Create return
GET    /api/v1/sales/returns                   # List returns
GET    /api/v1/sales/returns/{id}              # Get return details
```

### Sales Portal Pages

| Page | Route | Description |
|------|-------|-------------|
| Dashboard | `/sales` | Sales overview, targets |
| Dealers | `/sales/dealers` | Dealer CRUD |
| Dealer Detail | `/sales/dealers/{id}` | Ledger, orders, invoices |
| Orders | `/sales/orders` | Order management |
| Create Order | `/sales/orders/new` | New order form |
| Order Detail | `/sales/orders/{id}` | Order details & status |
| Targets | `/sales/targets` | Sales targets |
| Promotions | `/sales/promotions` | Manage promos |
| Returns | `/sales/returns` | Sales returns |

---

## 4. FACTORY PORTAL

**Purpose:** Production management, mixing, packing, quality control, dispatch

**URL Pattern:** `/factory/*`

**Roles Required:** `ROLE_ADMIN`, `ROLE_FACTORY`

### Modules

#### 4.1 Production Orders
```http
GET    /api/v1/factory/production-orders       # List production orders
POST   /api/v1/factory/production-orders       # Create production order
GET    /api/v1/factory/production-orders/{id}  # Get details
PUT    /api/v1/factory/production-orders/{id}/start   # Start production
PUT    /api/v1/factory/production-orders/{id}/complete # Complete
```

#### 4.2 Mixing (Raw Material → WIP)
```http
POST   /api/v1/factory/mixing                  # Record mixing batch
GET    /api/v1/factory/mixing/batches          # List mixing batches
```

**Mixing Request:**
```json
{
  "productionOrderId": 1,
  "batchCode": "MIX-2024-001",
  "productId": 1,
  "quantity": 100,
  "rawMaterials": [
    {"materialId": 1, "quantity": 50},
    {"materialId": 2, "quantity": 30}
  ],
  "operatorId": 5
}
```

**What happens:**
- Raw material inventory decreases
- WIP (Work in Progress) inventory increases
- Material consumption journal entry created

#### 4.3 Packing (WIP → Finished Goods)
```http
POST   /api/v1/factory/packing                 # Record packing
GET    /api/v1/factory/packing/slips           # List packing slips
GET    /api/v1/factory/packing/slips/{id}      # Get slip details
```

**Packing Request:**
```json
{
  "mixingBatchId": 1,
  "packingSlipNumber": "PS-2024-001",
  "finishedGoods": [
    {"productId": 1, "quantity": 100, "batchCode": "FG-2024-001"}
  ],
  "operatorId": 6
}
```

**What happens:**
- WIP inventory decreases
- Finished goods inventory increases
- Cost allocation journal entry created

#### 4.4 Cost Allocation
```http
POST   /api/v1/factory/cost-allocation         # Allocate costs to batch
GET    /api/v1/factory/batches/{id}/costs      # Get batch cost breakdown
```

**Cost Allocation Request:**
```json
{
  "batchCode": "FG-2024-001",
  "laborCost": 500.00,
  "overheadCost": 200.00,
  "otherCosts": 100.00
}
```

#### 4.5 Dispatch
```http
# Confirm dispatch (creates invoice + accounting entries)
POST   /api/v1/sales/dispatch/confirm
{
  "packingSlipId": 123,
  "orderId": 456,
  "adminOverrideCreditLimit": false,
  "lines": [
    {"lineId": 1, "quantityToShip": 10}
  ]
}
```

**What happens on Dispatch:**
1. ✅ Finished goods inventory decreases
2. ✅ COGS journal entry (Debit COGS, Credit Inventory)
3. ✅ AR journal entry (Debit Receivable, Credit Revenue + GST)
4. ✅ Invoice auto-generated
5. ✅ Dealer ledger updated
6. ✅ Order status → DISPATCHED

#### 4.6 Quality Control
```http
POST   /api/v1/factory/qc/check                # Record QC check
GET    /api/v1/factory/qc/reports              # QC reports
```

### Factory Portal Pages

| Page | Route | Description |
|------|-------|-------------|
| Dashboard | `/factory` | Production overview |
| Production Orders | `/factory/orders` | Production order list |
| Create Production | `/factory/orders/new` | New production order |
| Mixing | `/factory/mixing` | Mixing batches |
| Packing | `/factory/packing` | Packing operations |
| Dispatch | `/factory/dispatch` | Dispatch confirmation |
| QC | `/factory/qc` | Quality control |
| Inventory | `/factory/inventory` | Raw material & WIP stock |

### Dispatch Confirmation Modal

```
┌─────────────────────────────────────────────────────────────────┐
│              DISPATCH CONFIRMATION                               │
├─────────────────────────────────────────────────────────────────┤
│  Order: SO-2024-001          Dealer: ABC Paints Shop            │
│  Packing Slip: PS-2024-050   Delivery: 123 Market Street        │
├─────────────────────────────────────────────────────────────────┤
│  ITEMS TO DISPATCH                                              │
│  ┌─────────────────┬─────────┬─────────┬─────────┬──────────┐  │
│  │ Product         │ Ordered │ Packed  │ Ship    │ Price    │  │
│  ├─────────────────┼─────────┼─────────┼─────────┼──────────┤  │
│  │ Premium White   │ 10      │ 10      │ [10]    │ ₹500.00  │  │
│  │ Primer Grey     │ 5       │ 5       │ [5]     │ ₹350.00  │  │
│  └─────────────────┴─────────┴─────────┴─────────┴──────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  Subtotal:     ₹6,750.00                                        │
│  GST (18%):    ₹1,215.00                                        │
│  TOTAL:        ₹7,965.00                                        │
├─────────────────────────────────────────────────────────────────┤
│  Vehicle No:   [_______________]                                │
│  Driver:       [_______________]                                │
│  ☑ Send Invoice Email to dealer@example.com                     │
├─────────────────────────────────────────────────────────────────┤
│  [Cancel]                            [Confirm & Generate Invoice]│
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. DEALER PORTAL

**Purpose:** External portal for dealers to view their orders, invoices, payments, ledger

**URL Pattern:** `/dealer/*` (separate app or protected routes)

**Roles Required:** `ROLE_DEALER`

### Modules

#### 5.1 Dealer Dashboard
```http
GET    /api/v1/portal/dashboard                # Dealer's dashboard stats
```

#### 5.2 My Orders
```http
GET    /api/v1/portal/orders                   # My orders
GET    /api/v1/portal/orders/{id}              # Order details
POST   /api/v1/portal/orders                   # Place new order (if allowed)
```

#### 5.3 My Invoices
```http
GET    /api/v1/portal/invoices                 # My invoices
GET    /api/v1/portal/invoices/{id}            # Invoice details
GET    /api/v1/portal/invoices/{id}/pdf        # Download PDF
```

#### 5.4 My Ledger
```http
GET    /api/v1/portal/ledger                   # Account ledger
GET    /api/v1/portal/statement                # Account statement PDF
GET    /api/v1/portal/balance                  # Current outstanding
```

#### 5.5 My Payments
```http
GET    /api/v1/portal/payments                 # Payment history
```

### Dealer Portal Pages

| Page | Route | Description |
|------|-------|-------------|
| Dashboard | `/dealer` | Overview, outstanding |
| Orders | `/dealer/orders` | Order history |
| Invoices | `/dealer/invoices` | Invoice list |
| Ledger | `/dealer/ledger` | Account transactions |
| Statement | `/dealer/statement` | Download statement |

---

## Module Connections Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA FLOW                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ADMIN                                                                       │
│    │                                                                         │
│    ├─── Users/Roles ───────────────────────────────────────────────────────►│
│    │                                                                         │
│    └─── HR/Payroll ──────────► ACCOUNTING (Salary Journal Entries)          │
│                                     │                                        │
│  PURCHASING                         │                                        │
│    │                                │                                        │
│    ├─── Suppliers ─────────────────►│                                        │
│    ├─── Purchase Orders ───────────►│ (AP Journal on GRN)                   │
│    └─── Goods Receipt ─────────────►│                                        │
│              │                      │                                        │
│              ▼                      │                                        │
│  FACTORY                            │                                        │
│    │                                │                                        │
│    ├─── Mixing ────────────────────►│ (Material Consumption Journal)        │
│    ├─── Packing ───────────────────►│ (Cost Allocation Journal)             │
│    └─── Dispatch ──────────────────►│ (COGS + AR Journal + Invoice)         │
│              │                      │                                        │
│              ▼                      │                                        │
│  SALES                              │                                        │
│    │                                │                                        │
│    ├─── Dealers ───────────────────►│ (Receivable Accounts)                 │
│    ├─── Orders ────────────────────►│ (Reservation, Status)                 │
│    └─── Returns ───────────────────►│ (Credit Note Journal)                 │
│              │                      │                                        │
│              ▼                      │                                        │
│  INVOICES                           │                                        │
│    │                                │                                        │
│    ├─── Auto-generated on Dispatch ─┤                                        │
│    └─── Payments ──────────────────►│ (Settlement Journal)                  │
│                                     │                                        │
│                                     ▼                                        │
│                              ACCOUNTING                                      │
│                                     │                                        │
│                    ┌────────────────┼────────────────┐                      │
│                    ▼                ▼                ▼                      │
│              Trial Balance    Profit & Loss    Balance Sheet                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Authentication Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                    LOGIN FLOW                                     │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│   1. POST /api/v1/auth/login                                     │
│      Body: { email, password }                                    │
│                                                                   │
│   2. Response:                                                    │
│      ├─ Success (no MFA): { accessToken, refreshToken, user }    │
│      └─ MFA Required: { mfaRequired: true, mfaToken }            │
│                                                                   │
│   3. If MFA Required:                                             │
│      POST /api/v1/auth/mfa/verify                                │
│      Body: { mfaToken, code }                                     │
│      Response: { accessToken, refreshToken, user }               │
│                                                                   │
│   4. Store tokens in localStorage/cookie                          │
│                                                                   │
│   5. All API calls include:                                       │
│      Authorization: Bearer <accessToken>                          │
│      X-Company-Id: <companyId>                                    │
│                                                                   │
│   6. On 401 error:                                                │
│      POST /api/v1/auth/refresh                                   │
│      Body: { refreshToken }                                       │
│                                                                   │
│   7. Logout:                                                      │
│      POST /api/v1/auth/logout                                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Role-Based Access Control

| Portal | Required Role | Key Permissions |
|--------|---------------|-----------------|
| Admin | `ROLE_ADMIN` | `users.*`, `roles.*`, `companies.*`, `hr.*` |
| Accounting | `ROLE_ACCOUNTING` | `accounts.*`, `journals.*`, `reports.*`, `invoices.*` |
| Sales | `ROLE_SALES` | `dealers.*`, `orders.*`, `targets.*` |
| Factory | `ROLE_FACTORY` | `production.*`, `mixing.*`, `packing.*`, `dispatch.*` |
| Dealer | `ROLE_DEALER` | `portal.*` (limited to own data) |

---

## API Response Format

**Success:**
```json
{
  "success": true,
  "data": { ... },
  "message": null,
  "errors": null
}
```

**Error:**
```json
{
  "success": false,
  "data": null,
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Email is required"},
    {"field": "phone", "message": "Invalid phone format"}
  ]
}
```

**Paginated List:**
```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 150,
    "totalPages": 8,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false
  }
}
```

---

## Quick Reference: Key Endpoints by Portal

### Admin Portal
| Action | Endpoint |
|--------|----------|
| List users | `GET /api/v1/auth/users` |
| Create user | `POST /api/v1/auth/users` |
| List employees | `GET /api/v1/hr/employees` |
| Run payroll | `POST /api/v1/hr/payroll/process` |

### Accounting Portal
| Action | Endpoint |
|--------|----------|
| List accounts | `GET /api/v1/accounting/accounts` |
| Create journal | `POST /api/v1/accounting/journal-entries` |
| Trial balance | `GET /api/v1/accounting/trial-balance` |
| List invoices | `GET /api/v1/invoices` |
| Receive payment | `POST /api/v1/accounting/settlements/dealer` |
| List suppliers | `GET /api/v1/suppliers` |
| Create PO | `POST /api/v1/purchasing/orders` |

### Sales Portal
| Action | Endpoint |
|--------|----------|
| List dealers | `GET /api/v1/dealers` |
| Create dealer | `POST /api/v1/dealers` |
| List orders | `GET /api/v1/sales/orders` |
| Create order | `POST /api/v1/sales/orders` |
| Dealer ledger | `GET /api/v1/dealers/{id}/ledger` |

### Factory Portal
| Action | Endpoint |
|--------|----------|
| Production orders | `GET /api/v1/factory/production-orders` |
| Record mixing | `POST /api/v1/factory/mixing` |
| Record packing | `POST /api/v1/factory/packing` |
| Confirm dispatch | `POST /api/v1/sales/dispatch/confirm` |

### Dealer Portal
| Action | Endpoint |
|--------|----------|
| My orders | `GET /api/v1/portal/orders` |
| My invoices | `GET /api/v1/portal/invoices` |
| My ledger | `GET /api/v1/portal/ledger` |
| Download invoice | `GET /api/v1/portal/invoices/{id}/pdf` |

---

## Contact

**Backend Running:** `http://localhost:8081`  
**Swagger UI:** `http://localhost:8081/swagger-ui/index.html`  
**Test Credentials:** `mdanas7869292@gmail.com` / `Admin@12345`
