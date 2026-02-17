# Sales Portal Endpoint Map (V1)

Source of truth:
- `openapi.json`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/SalesController.java`

Portal: `SALES`  
Scope: dealer directory, order lifecycle, dispatch, credit approvals, promotions, targets

## 1) Core Endpoint Packet

### Dealer directory support
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/sales/dealers` | dealer lookup list |
| GET | `/api/v1/sales/dealers/search` | dealer search |

### Sales order lifecycle
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/sales/orders` | list orders |
| POST | `/api/v1/sales/orders` | create order |
| PUT | `/api/v1/sales/orders/{id}` | update order |
| DELETE | `/api/v1/sales/orders/{id}` | delete order |
| POST | `/api/v1/sales/orders/{id}/confirm` | confirm order |
| POST | `/api/v1/sales/orders/{id}/cancel` | cancel order |
| PATCH | `/api/v1/sales/orders/{id}/status` | status update |

### Dispatch and invoicing bridge
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/sales/dispatch/confirm` | dispatch confirmation and posting bridge |
| POST | `/api/v1/sales/dispatch/reconcile-order-markers` | repair stale dispatch markers |
| GET | `/api/v1/invoices/dealers/{dealerId}` | dealer invoice listing |
| GET | `/api/v1/invoices/{id}` | invoice detail |
| GET | `/api/v1/invoices/{id}/pdf` | invoice PDF |

### Credit and governance
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/sales/credit-requests` | credit request queue |
| POST | `/api/v1/sales/credit-requests` | create request |
| PUT | `/api/v1/sales/credit-requests/{id}` | edit request |
| POST | `/api/v1/sales/credit-requests/{id}/approve` | approve request with reason |
| POST | `/api/v1/sales/credit-requests/{id}/reject` | reject request with reason |

### Promotions and targets
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/sales/promotions` | list promotions |
| POST | `/api/v1/sales/promotions` | create promotion |
| PUT | `/api/v1/sales/promotions/{id}` | update promotion |
| DELETE | `/api/v1/sales/promotions/{id}` | delete promotion |
| GET | `/api/v1/sales/targets` | list targets |
| POST | `/api/v1/sales/targets` | create target |
| PUT | `/api/v1/sales/targets/{id}` | update target |
| DELETE | `/api/v1/sales/targets/{id}` | delete target (reason required by service policy) |

## 2) Required Write-Flow Contracts

1. Order create/update must preserve idempotency behavior (`Idempotency-Key` support on create).
2. Credit decision endpoints require non-empty reason payload.
3. Sales target create/update requires `assignee` and `changeReason`.
4. Target delete must submit `reason` query to satisfy governance checks.

## 3) Role Matrix (Fail-Closed)

| Flow | Primary Roles | Notes |
|---|---|---|
| order lifecycle | `ROLE_SALES`, `ROLE_ADMIN` | read includes accounting/factory on list |
| credit approvals | `ROLE_ADMIN`, `ROLE_ACCOUNTING` | reason required |
| promotions | `ROLE_SALES`, `ROLE_ADMIN` | dealer can read promotions only |
| target governance | `ROLE_ADMIN` (write), `ROLE_SALES` (read) | self-assignment blocked by policy |

## 4) UX Contracts
- Present order lifecycle as timeline steps, not disconnected actions.
- On denial, always show actionable reason (credit policy, role boundary, tenant boundary).
- For approval/rejection and target changes, force reason inputs before submit.
- After dispatch confirmation, refresh order + invoice panels from backend result.

## 5) Error Contract
- `401`: relogin required.
- `403`: role or tenant boundary.
- `409`: idempotency/payload conflict or state conflict.
- `422`: validation contract mismatch.

## 6) Frontend Implementation Checklist
- Enforce reason capture UI for all governance writes.
- Treat cancel/confirm/status as state transitions with confirmation step.
- Normalize conflict handling UI for idempotency and stale status cases.
- Ensure sales screens do not expose superadmin-only or cross-tenant actions.
