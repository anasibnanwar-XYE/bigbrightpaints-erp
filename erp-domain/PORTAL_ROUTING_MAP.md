# Portal Routing Map

**Which API endpoints belong to which Portal**

---

## 1. ADMIN PORTAL

```
/admin/*  →  Frontend Routes
```

### User Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/auth/users` | List all users |
| POST | `/api/v1/auth/users` | Create user |
| GET | `/api/v1/auth/users/{id}` | Get user |
| PUT | `/api/v1/auth/users/{id}` | Update user |
| DELETE | `/api/v1/auth/users/{id}` | Delete user |
| POST | `/api/v1/auth/users/{id}/reset-password` | Reset password |

### Role & Permission Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/rbac/roles` | List roles |
| POST | `/api/v1/rbac/roles` | Create role |
| GET | `/api/v1/rbac/roles/{id}` | Get role |
| PUT | `/api/v1/rbac/roles/{id}` | Update role |
| GET | `/api/v1/rbac/permissions` | List permissions |

### Company Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/companies` | List companies |
| POST | `/api/v1/companies` | Create company |
| GET | `/api/v1/companies/{id}` | Get company |
| PUT | `/api/v1/companies/{id}` | Update company |

### HR & Employees
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/hr/employees` | List employees |
| POST | `/api/v1/hr/employees` | Create employee |
| GET | `/api/v1/hr/employees/{id}` | Get employee |
| PUT | `/api/v1/hr/employees/{id}` | Update employee |
| DELETE | `/api/v1/hr/employees/{id}` | Delete employee |

### Payroll
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/hr/payroll/process` | Run payroll |
| GET | `/api/v1/hr/payroll/runs` | List payroll runs |
| GET | `/api/v1/hr/payroll/slips/{employeeId}` | Get pay slips |

### System Settings
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/settings` | Get settings |
| PUT | `/api/v1/settings` | Update settings |

---

## 2. ACCOUNTING PORTAL

```
/accounting/*  →  Frontend Routes
```

### Chart of Accounts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/accounting/accounts` | List accounts |
| POST | `/api/v1/accounting/accounts` | Create account |
| GET | `/api/v1/accounting/accounts/{id}` | Get account |
| PUT | `/api/v1/accounting/accounts/{id}` | Update account |
| DELETE | `/api/v1/accounting/accounts/{id}` | Deactivate account |
| GET | `/api/v1/accounting/accounts/tree` | Account hierarchy |

### Journal Entries
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/accounting/journal-entries` | List entries |
| POST | `/api/v1/accounting/journal-entries` | Create entry |
| GET | `/api/v1/accounting/journal-entries/{id}` | Get entry |
| POST | `/api/v1/accounting/journal-entries/{id}/reverse` | Reverse entry |

### Accounting Periods
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/accounting/periods` | List periods |
| POST | `/api/v1/accounting/periods` | Create period |
| POST | `/api/v1/accounting/periods/{id}/close` | Close period |
| POST | `/api/v1/accounting/periods/{id}/lock` | Lock period |
| POST | `/api/v1/accounting/periods/{id}/reopen` | Reopen period |

### Financial Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/accounting/trial-balance` | Trial balance |
| GET | `/api/v1/accounting/profit-loss` | P&L statement |
| GET | `/api/v1/accounting/balance-sheet` | Balance sheet |
| GET | `/api/v1/accounting/ledger/{accountId}` | Account ledger |
| GET | `/api/v1/accounting/cash-flow` | Cash flow |

### Suppliers (Purchase)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/suppliers` | List suppliers |
| POST | `/api/v1/suppliers` | Create supplier |
| GET | `/api/v1/suppliers/{id}` | Get supplier |
| PUT | `/api/v1/suppliers/{id}` | Update supplier |

### Purchase Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/purchasing/orders` | List POs |
| POST | `/api/v1/purchasing/orders` | Create PO |
| GET | `/api/v1/purchasing/orders/{id}` | Get PO |
| PUT | `/api/v1/purchasing/orders/{id}/approve` | Approve PO |

### Goods Receipt
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/purchasing/receipts` | List GRNs |
| POST | `/api/v1/purchasing/receipts` | Record GRN |

### Invoices
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/invoices` | List invoices |
| GET | `/api/v1/invoices/{id}` | Get invoice |
| GET | `/api/v1/invoices/{id}/pdf` | Download PDF |
| POST | `/api/v1/invoices/{id}/email` | Send email |
| GET | `/api/v1/invoices/dealers/{dealerId}` | Dealer invoices |

### Payments & Settlements
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/accounting/settlements` | List settlements |
| POST | `/api/v1/accounting/settlements/dealer` | Receive payment |
| POST | `/api/v1/accounting/settlements/supplier` | Make payment |

### GST
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/accounting/gst/summary` | GST summary |
| GET | `/api/v1/accounting/gst/input-credit` | Input credit |
| GET | `/api/v1/accounting/gst/output-liability` | Output liability |

---

## 3. SALES PORTAL

```
/sales/*  →  Frontend Routes
```

### Dealers
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/dealers` | List dealers |
| POST | `/api/v1/dealers` | Create dealer |
| GET | `/api/v1/dealers/{id}` | Get dealer |
| PUT | `/api/v1/dealers/{id}` | Update dealer |
| GET | `/api/v1/dealers/{id}/ledger` | Dealer ledger |
| GET | `/api/v1/dealers/{id}/statement` | Account statement |

### Sales Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sales/orders` | List orders |
| POST | `/api/v1/sales/orders` | Create order |
| GET | `/api/v1/sales/orders/{id}` | Get order |
| PUT | `/api/v1/sales/orders/{id}` | Update order |
| POST | `/api/v1/sales/orders/{id}/confirm` | Confirm order |
| POST | `/api/v1/sales/orders/{id}/cancel` | Cancel order |

### Sales Targets
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sales/targets` | List targets |
| POST | `/api/v1/sales/targets` | Create target |
| PUT | `/api/v1/sales/targets/{id}` | Update target |
| DELETE | `/api/v1/sales/targets/{id}` | Delete target |

### Promotions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sales/promotions` | List promotions |
| POST | `/api/v1/sales/promotions` | Create promotion |
| PUT | `/api/v1/sales/promotions/{id}` | Update promotion |
| DELETE | `/api/v1/sales/promotions/{id}` | Delete promotion |

### Sales Returns
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sales/returns` | List returns |
| POST | `/api/v1/sales/returns` | Create return |
| GET | `/api/v1/sales/returns/{id}` | Get return |

---

## 4. FACTORY PORTAL

```
/factory/*  →  Frontend Routes
```

### Production Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/factory/production-orders` | List orders |
| POST | `/api/v1/factory/production-orders` | Create order |
| GET | `/api/v1/factory/production-orders/{id}` | Get order |
| PUT | `/api/v1/factory/production-orders/{id}/start` | Start production |
| PUT | `/api/v1/factory/production-orders/{id}/complete` | Complete |

### Mixing
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/factory/mixing/batches` | List mixing batches |
| POST | `/api/v1/factory/mixing` | Record mixing |

### Packing
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/factory/packing/slips` | List packing slips |
| POST | `/api/v1/factory/packing` | Record packing |
| GET | `/api/v1/factory/packing/slips/{id}` | Get slip |

### Cost Allocation
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/factory/cost-allocation` | Allocate costs |
| GET | `/api/v1/factory/batches/{id}/costs` | Batch costs |

### Dispatch
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/sales/dispatch/confirm` | Confirm dispatch |
| POST | `/api/v1/orchestrator/factory/dispatch/{batchId}` | Dispatch batch |
| POST | `/api/v1/orchestrator/dispatch/{orderId}` | Quick dispatch |

### Inventory (Raw Materials & WIP)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/inventory/raw-materials` | List raw materials |
| GET | `/api/v1/inventory/finished-goods` | List finished goods |
| GET | `/api/v1/inventory/stock-levels` | Stock levels |
| GET | `/api/v1/inventory/batches` | List batches |

---

## 5. DEALER PORTAL (External)

```
/dealer/*  →  Frontend Routes (separate app or protected)
```

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/portal/dashboard` | Dealer dashboard |
| GET | `/api/v1/portal/insights` | Business insights |

### My Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/portal/orders` | My orders |
| GET | `/api/v1/portal/orders/{id}` | Order details |
| POST | `/api/v1/portal/orders` | Place order |

### My Invoices
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/portal/invoices` | My invoices |
| GET | `/api/v1/portal/invoices/{id}` | Invoice details |
| GET | `/api/v1/portal/invoices/{id}/pdf` | Download PDF |

### My Ledger
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/portal/ledger` | Account ledger |
| GET | `/api/v1/portal/statement` | Account statement |
| GET | `/api/v1/portal/balance` | Current balance |

### My Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/portal/payments` | Payment history |

---

## SHARED (All Portals)

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Refresh token |
| POST | `/api/v1/auth/logout` | Logout |
| POST | `/api/v1/auth/password/forgot` | Forgot password |
| POST | `/api/v1/auth/password/reset` | Reset password |

### MFA
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/mfa/enable` | Enable MFA |
| POST | `/api/v1/mfa/verify` | Verify MFA setup |
| POST | `/api/v1/auth/mfa/verify` | Login with MFA |
| POST | `/api/v1/mfa/disable` | Disable MFA |

### Profile
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/profile` | Get my profile |
| PUT | `/api/v1/profile` | Update profile |
| PUT | `/api/v1/profile/password` | Change password |

### Health & Info
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/info` | App info |

---

## Quick Reference by Frontend Route

| Frontend Route | Backend Endpoints |
|----------------|-------------------|
| `/admin/users` | `/api/v1/auth/users/*` |
| `/admin/roles` | `/api/v1/rbac/*` |
| `/admin/companies` | `/api/v1/companies/*` |
| `/admin/hr/employees` | `/api/v1/hr/employees/*` |
| `/admin/hr/payroll` | `/api/v1/hr/payroll/*` |
| `/accounting/accounts` | `/api/v1/accounting/accounts/*` |
| `/accounting/journals` | `/api/v1/accounting/journal-entries/*` |
| `/accounting/reports/*` | `/api/v1/accounting/trial-balance`, `/profit-loss`, `/balance-sheet` |
| `/accounting/suppliers` | `/api/v1/suppliers/*` |
| `/accounting/purchases` | `/api/v1/purchasing/*` |
| `/accounting/invoices` | `/api/v1/invoices/*` |
| `/accounting/payments` | `/api/v1/accounting/settlements/*` |
| `/sales/dealers` | `/api/v1/dealers/*` |
| `/sales/orders` | `/api/v1/sales/orders/*` |
| `/sales/targets` | `/api/v1/sales/targets/*` |
| `/factory/orders` | `/api/v1/factory/production-orders/*` |
| `/factory/mixing` | `/api/v1/factory/mixing/*` |
| `/factory/packing` | `/api/v1/factory/packing/*` |
| `/factory/dispatch` | `/api/v1/sales/dispatch/*`, `/api/v1/orchestrator/dispatch/*` |
| `/dealer/*` | `/api/v1/portal/*` |
