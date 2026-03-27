# Sales Module - Canonical Flows

// Order-to-Cash (O2C) Process
// This document describes the canonical business flows in the Sales module.

---

## Overview

The Sales module implements the complete Order-to-Cash (O2C) cycle, from initial order creation through payment collection. This document describes the key flows and their integration points with other modules.

---

## Primary Flow: Order-to-Cash (O2C)

// The complete O2C flow from order creation to payment settlement.

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                        ORDER-TO-CASH (O2C) FLOW                                           │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│   ┌──────────┐    ┌───────────┐    ┌──────────────┐    ┌───────────────┐               │
│   │  DRAFT   │───▶│ CONFIRMED │───▶│   RESERVED   │───▶│   DISPATCHED  │               │
│   └──────────┘    └───────────┘    └──────────────┘    └───────────────┘               │
│        │               │                   │                    │                          │
│        │               │                   │                    │                          │
│        ▼               ▼                   ▼                    ▼                          │
│   [Credit Check]  [Inventory]       [Dispatch]           [Invoice]                      │
│        │               │                   │                    │                          │
│        ▼               ▼                   ▼                    ▼                          │
│   ┌──────────────────────────────────────────────────────────────────────────┐           │
│   │                            INVOICED                                      │           │
│   └──────────────────────────────────────────────────────────────────────────┘           │
│                                    │                                                         │
│                                    ▼                                                         │
│                           ┌───────────────┐                                                │
│                           │    SETTLED    │                                                │
│                           └───────────────┘                                                │
│                                                                                              │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Phase 1: Order Creation
// **Entry Point:** POST /api/v1/sales/orders
// **Service:** SalesOrderCrudService.createOrder()
// **Core Engine:** SalesCoreEngine.createOrder()
```
1. Validate request payload
2. Resolve dealer (lock for credit check)
3. Calculate order amounts (subtotal, GST, total)
4. Enforce credit limit policy
   - Get current exposure (ledger balance + pending orders)
   - Compare against dealer credit limit
   - Throw CreditLimitExceededException if exceeded
5. Generate order number (OrderNumberService)
6. Create SalesOrder entity with items
7. Record initial status history (DRAFT)
8. Assess commercial availability (SalesProformaBoundaryService)
   - Check inventory for each line item
   - Sync production requirements with factory module
9. Transition to RESERVED or PENDING_PRODUCTION
10. Publish SalesOrderCreatedEvent
```

### Phase 2: Order Confirmation
// **Entry Point:** POST /api/v1/sales/orders/{id}/confirm
// **Service:** SalesOrderLifecycleService.confirmOrder()
```
1. Load order with pessimistic lock
2. Validate current status (DRAFT, RESERVED, etc.)
3. Validate order has positive total
4. Enforce credit limit (final check before fulfillment)
5. Validate stock availability
6. Transition status to CONFIRMED
7. Record status history
```

### Phase 3: Inventory Reservation
// **Service:** SalesFulfillmentService.reserveForOrder()
// **Integration:** FinishedGoodsService
```
1. For each order line item:
   a. Find finished good by product code
   b. Find available batches (FIFO)
   c. Reserve quantity from batches
   d. Create inventory reservation records
2. If any shortages:
   - Transition order to PENDING_INVENTORY or PENDING_PRODUCTION
   - Create factory tasks for production requirements
3. If fully reserved:
   - Transition order to RESERVED
```

### Phase 4: Dispatch Confirmation
// **Entry Point:** POST /api/v1/sales/dispatch/confirm
// **Service:** SalesDispatchReconciliationService.confirmDispatch()
// **Core Engine:** SalesCoreEngine.confirmDispatch()
```
1. Validate dispatch request
   - Packaging slip exists and in correct status
   - All line items have valid batch assignments
   - Quantities match order
2. Check credit limit (with override support)
   - If exceeds limit and no override: fail
   - If override approved: proceed with override
3. Post inventory movements (DISPATCH type)
   - Update batch quantities
   - Create inventory movement records
4. Post COGS journal entry
   - Debit: COGS account
   - Credit: Inventory account
   - Use actual batch costs
5. Post AR journal entry
   - Debit: Dealer receivable account
   - Credit: Sales revenue account
   - Credit: GST output tax account
6. Generate invoice
   - Create Invoice entity with lines
   - Link to sales order
   - Set invoice number
7. Update order markers
   - Set salesJournalEntryId
   - Set cogsJournalEntryId
   - Set fulfillmentInvoiceId
8. Transition order to DISPATCHED/INVOICED
```

### Phase 5: Payment Collection
// **Integration:** Accounting module handles payments
// **Trigger:** Payment recorded against invoice
```
1. Payment received against invoice
2. Update dealer ledger (reduce outstanding)
3. Update invoice outstanding amount
4. If fully paid: transition order to SETTLED
```

---

## Secondary Flows
// ### Credit Limit Increase Request Flow
// Permanently increases a dealer's credit limit after approval.
```
┌───────────────────────────────────────────────────────────────┐
│               CREDIT LIMIT INCREASE FLOW                        │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│   Dealer/Sales    ─────────▶  Admin/Accounting                │
│        │                           │                           │
│        ▼                           ▼                           │
│   ┌─────────────┐           ┌─────────────┐                  │
│   │   Submit    │           │   Review    │                  │
│   │   Request   │──────────▶│   Request   │                  │
│   └─────────────┘           └─────────────┘                  │
│                                    │                          │
│                          ┌────────┴────────┐                  │
│                          ▼                 ▼                  │
│                    ┌──────────┐       ┌──────────┐            │
│                    │ Approve  │       │ Reject   │            │
│                    └──────────┘       └──────────┘            │
│                          │                                   │
│                          ▼                                   │
│                    ┌──────────────┐                          │
│                    │ Update Dealer│                          │
│                    │ Credit Limit │                          │
│                    └──────────────┘                          │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

**Entry Points:**
- Internal: POST /api/v1/credit/limit-requests
- Dealer Portal: POST /api/v1/dealer-portal/credit-limit-requests

**Workflow:**
1. Request created with dealer ID, amount, reason
2. Status: PENDING
3. Admin reviews and approves/rejects
4. On approval: Dealer.creditLimit += amountRequested
5. Audit log created

---

### Credit Limit Override Flow
// Temporary one-time exception for a specific dispatch.
```
┌───────────────────────────────────────────────────────────────┐
│               CREDIT LIMIT OVERRIDE FLOW                        │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│   Dispatch Attempt  ────────▶  Credit Check                   │
│        │                           │                           │
│        │                    Exceeds Limit                      │
│        │                           │                           │
│        ▼                           ▼                           │
│   ┌─────────────┐           ┌─────────────────┐               │
│   │   Create    │           │ Block Dispatch  │               │
│   │   Override  │           │ Awaiting Review │               │
│   │   Request   │──────────▶│                 │               │
│   └─────────────┘           └─────────────────┘               │
│                                    │                          │
│                          ┌────────┴────────┐                  │
│                          ▼                 ▼                  │
│                    ┌──────────┐       ┌──────────┐            │
│                    │ Approve  │       │ Reject   │            │
│                    │(24h exp)│       │          │            │
│                    └──────────┘       └──────────┘            │
│                          │                                   │
│                          ▼                                   │
│                    ┌──────────────┐                          │
│                    │  Allow       │                          │
│                    │  Dispatch    │                          │
│                    └──────────────┘                          │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

**Key Features:**
- Maker-checker boundary enforced
- Expiration time (default 24 hours)
- Linked to specific packaging slip or order
- One-time use only

---

### Sales Return Flow
// Process for handling returned goods from invoiced orders.
```
┌───────────────────────────────────────────────────────────────┐
│               SALES RETURN FLOW                                 │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐       │
│   │   Preview   │───▶│   Validate  │───▶│   Process   │       │
│   │   Return    │    │   Return    │    │   Return    │       │
│   └─────────────┘    └─────────────┘    └─────────────┘       │
│                                                │               │
│                          ┌────────────────────┼──────────────┐│
│                          ▼                    ▼              ▼│
│                    ┌──────────┐        ┌──────────┐  ┌──────────┐
│                    │  Restock  │        │  Reverse │  │  Update  │
│                    │ Inventory│        │   COGS   │  │  Ledger  │
│                    └──────────┘        └──────────┘  └──────────┘
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

**Steps:**
1. Preview return (calculate amounts)
2. Validate quantities against original invoice
3. Create inventory movement (RETURN type)
4. Restock inventory (create return batch)
5. Post sales return journal (reverse revenue + tax)
6. Post COGS reversal journal
7. Update dealer ledger

---

### Dunning Flow
// Automated overdue account management.
```
┌───────────────────────────────────────────────────────────────┐
│               DUNNING FLOW                                     │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│   Scheduled Job (Daily 3:15 AM)                               │
│        │                                                       │
│        ▼                                                       │
│   ┌─────────────────────────────────────────────┐            │
│   │  For each company:                          │            │
│   │    For each dealer:                         │            │
│   │      1. Calculate aging buckets             │            │
│   │      2. Check 45+ day bucket               │            │
│   │      3. If amount > 0:                     │            │
│   │         - Set status to ON_HOLD             │            │
│   │         - Send reminder email               │            │
│   └─────────────────────────────────────────────┘            │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## Integration Points
// | Module | Integration | Direction | Purpose |
// |--------|------------|-----------|---------|
// | Inventory | FinishedGoodsService | Outbound | Reserve/dispatch inventory |
// | Inventory | PackagingSlip | Inbound | Dispatch confirmation |
// | Invoice | InvoiceService | Outbound | Generate invoices |
// | Accounting | AccountingFacade | Outbound | Post journal entries |
// | Accounting | DealerLedgerService | Outbound | Dealer balance tracking |
// | Company | CompanyContextService | Inbound | Multi-tenancy |
// | Auth | UserAccount | Inbound | Portal users |
// | RBAC | RoleService | Outbound | Role management |
// | Factory | FactoryTaskRepository | Outbound | Production requirements |
// | Notification | EmailService | Outbound | Send emails |

---

## Transaction Boundaries
// Critical operations that require transaction management:

| Operation | Transaction Type | Isolation | Purpose |
|-----------|-----------------|-----------|---------|
| Order creation | REQUIRED | REPEATABLE_READ | Prevent concurrent modifications |
| Dispatch confirmation | REQUIRED | REPEATABLE_READ | Atomic inventory + accounting |
| Credit limit approval | REQUIRED | REPEATABLE_READ | Lock dealer during update |
| Order number generation | REQUIRES_NEW | - | Separate transaction for sequence |
| Sales return | REQUIRED | REPEATABLE_READ | Atomic inventory + accounting reversal |

---

## Event Publishing
// The module publishes domain events:

| Event | Trigger | Consumers |
|-------|---------|-----------|
| `SalesOrderCreatedEvent` | Order created | Async processors |
