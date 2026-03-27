# Inventory Module - Canonical Flows

## Overview

This document describes the canonical business flows in the Inventory module: inventory movement, batch traceability, dispatch workflow, and adjustments.

---

## Flow 1: Inventory Movement (Finished Goods)

### Trigger Points
- Production log completion → RECEIPT
- Sales order dispatch → DISPATCH
- Inventory adjustment → ADJUSTMENT_IN / ADJUSTMENT_OUT
- Opening stock import → RECEIPT

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           INVENTORY MOVEMENT FLOW                            │
└─────────────────────────────────────────────────────────────────────────────┘

┌───────────────┐     ┌──────────────────────┐     ┌─────────────────────────┐
│ Production    │     │ FinishedGoodBatch    │     │ InventoryMovement       │
│ Log Complete  │────▶│ (create/update)      │────▶│ (RECEIPT)               │
└───────────────┘     └──────────────────────┘     └─────────────────────────┘
                              │                            │
                              ▼                            ▼
                      ┌───────────────┐          ┌──────────────────────┐
                      │ FinishedGood  │          │ InventoryMovement    │
                      │ currentStock+=│          │ Event Published      │
                      └───────────────┘          └──────────────────────┘

┌───────────────┐     ┌──────────────────────┐     ┌─────────────────────────┐
│ Dispatch      │     │ InventoryReservation │     │ InventoryMovement       │
│ Confirmed     │────▶│ (fulfilled)          │────▶│ (DISPATCH)              │
└───────────────┘     └──────────────────────┘     └─────────────────────────┘
                              │                            │
                              ▼                            ▼
                      ┌───────────────┐          ┌──────────────────────┐
                      │ FinishedGood  │          │ Journal Entry        │
                      │ currentStock-=│          │ Posted (COGS)        │
                      │ reservedStock-=│         └──────────────────────┘
                      └───────────────┘
```

### Movement Types

| Type | Direction | Stock Impact | Description |
|------|-----------|--------------|-------------|
| RECEIPT | + | currentStock += qty | Production receipt, opening stock |
| DISPATCH | - | currentStock -=, reservedStock -= | Sales dispatch |
| RESERVE | - | reservedStock += qty | Sales order reservation |
| RELEASE | + | reservedStock -= qty | Cancel reservation |
| ADJUSTMENT_IN | + | currentStock += qty | Positive adjustment |
| ADJUSTMENT_OUT | - | currentStock -= qty | Negative adjustment |

### Reference Types

| Type | ReferenceId | Description |
|------|-------------|-------------|
| SALES_ORDER | order.id.toString() | Sales order dispatch/reservation |
| PRODUCTION_LOG | log.code | Production receipt |
| MANUFACTURING_ORDER | batch.publicId.toString() | Manual batch registration |
| OPENING_STOCK | batchCode | Opening stock import |
| RAW_MATERIAL_ADJUSTMENT | referenceNumber | Raw material adjustment |

---

## Flow 2: Batch Traceability

### Purpose
Provide complete audit trail from batch creation to consumption/dispatch.

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           BATCH TRACEABILITY FLOW                            │
└─────────────────────────────────────────────────────────────────────────────┘

                         ┌─────────────────────┐
                         │   Batch Created     │
                         │   (source: PROD/    │
                         │    PURCHASE/ADJ)    │
                         └──────────┬──────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────┐         ┌─────────────────┐         ┌─────────────────┐
│ Movement      │         │ Reservation     │         │ Dispatch        │
│ (RECEIPT)     │         │ (RESERVE)       │         │ (DISPATCH)      │
└───────┬───────┘         └────────┬────────┘         └────────┬────────┘
        │                          │                           │
        ▼                          ▼                           ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     InventoryMovement / RawMaterialMovement                │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ Movement 1: RECEIPT    │ qty=100 │ unitCost=50 │ source=production  │  │
│  │ Movement 2: RESERVE    │ qty=50  │ ref=SO-123  │                    │  │
│  │ Movement 3: DISPATCH   │ qty=50  │ ref=SO-123  │ journalId=456      │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │ Traceability Report │
                         │ - Batch source      │
                         │ - All movements     │
                         │ - Journal links     │
                         │ - Dispatch details  │
                         └─────────────────────┘
```

### API Endpoint

```
GET /api/v1/inventory/batches/{id}/movements?batchType=RAW_MATERIAL|FINISHED_GOOD
```

### Response Structure

```json
{
  "batchId": 123,
  "publicId": "uuid",
  "batchType": "FINISHED_GOOD",
  "productCode": "WHT-100ML-001",
  "productName": "White Paint 100ml",
  "batchCode": "BBP-FG-WHT100ML001-202403-001",
  "manufacturedAt": "2024-03-15T10:00:00Z",
  "expiryDate": "2025-03-15",
  "quantityTotal": 500.00,
  "quantityAvailable": 350.00,
  "unitCost": 150.00,
  "source": "production",
  "movements": [
    {
      "id": 1,
      "movementType": "RECEIPT",
      "quantity": 500.00,
      "unitCost": 150.00,
      "totalCost": 75000.00,
      "createdAt": "2024-03-15T10:00:00Z",
      "source": "production",
      "referenceType": "PRODUCTION_LOG",
      "referenceId": "PL-2024-001",
      "journalEntryId": null,
      "packingSlipId": null
    },
    {
      "id": 2,
      "movementType": "RESERVE",
      "quantity": 100.00,
      "unitCost": 150.00,
      "totalCost": 15000.00,
      "createdAt": "2024-03-16T14:00:00Z",
      "source": "sales",
      "referenceType": "SALES_ORDER",
      "referenceId": "123",
      "journalEntryId": null,
      "packingSlipId": null
    },
    {
      "id": 3,
      "movementType": "DISPATCH",
      "quantity": 50.00,
      "unitCost": 150.00,
      "totalCost": 7500.00,
      "createdAt": "2024-03-17T09:00:00Z",
      "source": "sales",
      "referenceType": "SALES_ORDER",
      "referenceId": "123",
      "journalEntryId": 789,
      "packingSlipId": 45
    }
  ]
}
```

---

## Flow 3: Dispatch Workflow

### Stages

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            DISPATCH WORKFLOW                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ SALES ORDER  │───▶│  RESERVE     │───▶│   PREVIEW    │───▶│  CONFIRM     │
│   CONFIRMED  │    │  INVENTORY   │    │   DISPATCH   │    │   DISPATCH   │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                           │                    │                    │
                           ▼                    ▼                    ▼
                    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
                    │ PackagingSlip│    │ Show GST     │    │ Post COGS    │
                    │   CREATED    │    │ Breakdown    │    │ Journal      │
                    │   RESERVED   │    │              │    │              │
                    └──────────────┘    └──────────────┘    └──────────────┘
                                                                      │
                                    ┌─────────────────────────────────┘
                                    ▼
                             ┌──────────────┐    ┌──────────────┐
                             │  BACKORDER   │───▶│  BACKORDER   │
                             │   DETECTED   │    │    SLIP      │
                             └──────────────┘    └──────────────┘
```

### Packaging Slip Status Transitions

```
PENDING ──────▶ PENDING_STOCK ──────▶ RESERVED ──────▶ DISPATCHED
    │                   │                  │
    │                   │                  └──────▶ PENDING_PRODUCTION
    │                   │                           (if shortage)
    │                   │
    └──────▶ PENDING_PRODUCTION (if no stock available)
    │
    └──────▶ CANCELLED
```

### Dispatch Confirmation Steps

1. **Lock packaging slip** - Prevent concurrent modifications
2. **Validate shipped quantities** - Cannot exceed ordered quantities
3. **Process each line:**
   - Deduct from `FinishedGood.currentStock`
   - Deduct from `FinishedGood.reservedStock`
   - Deduct from `FinishedGoodBatch.quantityTotal`
   - Record `DISPATCH` movement
   - Update reservation status
4. **Calculate COGS** - Using costing method (FIFO/LIFO/WAC)
5. **Post journal entry** - Debit COGS, Credit Inventory
6. **Update slip status** - DISPATCHED or PENDING_STOCK
7. **Create backorder slip** - If any backorder quantities remain

### Backorder Handling

```
┌─────────────────────────────────────────────────────────────────┐
│                        BACKORDER FLOW                           │
└─────────────────────────────────────────────────────────────────┘

                    Ordered: 100
                    Shipped: 60
                    Backorder: 40

                           │
                           ▼
              ┌────────────────────────┐
              │ Create Backorder Slip  │
              │ slipNumber += "-BO"    │
              │ status = BACKORDER     │
              │ isBackorder = true     │
              └────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │ Reserve for Backorder  │
              │ (when stock available) │
              └────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │ Cancel Backorder       │
              │ (if order cancelled)   │
              │ - Release reservations │
              │ - Restore batch qty    │
              │ - status = CANCELLED   │
              └────────────────────────┘
```

---

## Flow 4: Inventory Adjustment

### Adjustment Types

| Type | Direction | Description |
|------|-----------|-------------|
| RECOUNT_UP | + | Positive recount adjustment |
| RECOUNT_DOWN | - | Negative recount adjustment |
| DAMAGED | - | Damaged/lost stock |

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ADJUSTMENT WORKFLOW                                  │
└─────────────────────────────────────────────────────────────────────────────┘

┌───────────────┐     ┌──────────────────────┐     ┌─────────────────────────┐
│ Adjustment    │────▶│ Validate Stock       │────▶│ Create Adjustment       │
│ Request       │     │ (for decreases)      │     │ Record (DRAFT)          │
└───────────────┘     └──────────────────────┘     └─────────────────────────┘
                              │                            │
                              │                            ▼
                              │                   ┌─────────────────────────┐
                              │                   │ Apply Stock Changes     │
                              │                   │ - Update FG stock       │
                              │                   │ - Create/Update batches │
                              │                   │ - Record movements      │
                              │                   └─────────────────────────┘
                              │                            │
                              ▼                            ▼
                      ┌───────────────┐          ┌──────────────────────┐
                      │ Insufficient  │          │ Post Journal Entry   │
                      │ Stock Error   │          │ - Debit/Credit based │
                      └───────────────┘          │   on direction       │
                                                 └──────────────────────┘
                                                            │
                                                            ▼
                                                 ┌──────────────────────┐
                                                 │ Update Status        │
                                                 │ status = POSTED      │
                                                 │ journalEntryId set   │
                                                 └──────────────────────┘
```

### Costing Method Impact

| Method | Batch Selection | Unit Cost Source |
|--------|----------------|------------------|
| FIFO | Oldest batches first | Batch unit cost |
| LIFO | Newest batches first | Batch unit cost |
| WAC | Any available batch | Weighted average cost |

### Journal Entry Structure

**Positive Adjustment (RECOUNT_UP):**
```
Debit:  Inventory Account (valuation)  = total amount
Credit: Adjustment Account             = total amount
```

**Negative Adjustment (RECOUNT_DOWN, DAMAGED):**
```
Debit:  Adjustment Account             = total amount
Credit: Inventory Account (valuation)  = total amount
```

---

## Flow 5: Opening Stock Import

### Purpose
Bulk import of opening stock during system migration or onboarding.

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      OPENING STOCK IMPORT FLOW                               │
└─────────────────────────────────────────────────────────────────────────────┘

┌───────────────┐     ┌──────────────────────┐     ┌─────────────────────────┐
│ Upload CSV    │────▶│ Validate SKU         │────▶│ Process Each Row        │
│ File          │     │ Readiness            │     │                         │
└───────────────┘     └──────────────────────┘     └─────────────────────────┘
       │                      │                            │
       │                      ▼                            ▼
       │              ┌───────────────┐          ┌──────────────────────┐
       │              │ Check:        │          │ For each row:        │
       │              │ - catalog     │          │ - Find material/FG   │
       │              │ - inventory   │          │ - Create batch       │
       │              │ - production  │          │ - Update stock       │
       │              │ - sales       │          │ - Record movement    │
       │              └───────────────┘          └──────────────────────┘
       │                                                   │
       │                                                   ▼
       │                                          ┌──────────────────────┐
       │                                          │ Post Opening Balance │
       │                                          │ Journal Entry        │
       │                                          │ (all lines totaled)  │
       │                                          └──────────────────────┘
       │                                                   │
       ▼                                                   ▼
┌───────────────┐                                 ┌──────────────────────┐
│ CSV Format:   │                                 │ Return Response      │
│ type, sku,    │                                 │ - rows processed     │
│ unit, batch   │                                 │ - batches created    │
│ qty, cost,    │                                 │ - errors (if any)    │
│ mfg, expiry   │                                 └──────────────────────┘
└───────────────┘
```

### CSV Format

```csv
type,sku,unit,batch_code,quantity,unit_cost,manufactured_at,expiry_date
RAW_MATERIAL,RM-001,KG,BATCH-001,100,50.00,2024-01-01,2025-01-01
FINISHED_GOOD,FG-001,UNIT,BATCH-002,50,150.00,2024-01-01,2025-01-01
PACKAGING_RAW_MATERIAL,Pkg-001,UNIT,BATCH-003,200,10.00,2024-01-01,2025-01-01
```

### Idempotency

- Requires `Idempotency-Key` header
- Requires `openingStockBatchKey` parameter
- Prevents duplicate imports
- Allows retry with same key

---

## Events

### InventoryMovementEvent

**Published when:** Inventory movement is recorded
**Purpose:** Enable downstream analytics and notifications

```java
{
  "companyId": 1,
  "movementType": "DISPATCH",
  "inventoryType": "FINISHED_GOOD",
  "itemId": 123,
  "itemCode": "FG-001",
  "itemName": "Product Name",
  "quantity": 50,
  "unitCost": 150.00,
  "totalCost": 7500.00,
  "movementId": 456,
  "referenceNumber": "SALES_ORDER-123",
  "movementDate": "2024-03-15",
  "relatedEntityId": 123,
  "relatedEntityType": "SALES_ORDER"
}
```

### InventoryValuationChangedEvent

**Published when:** Stock valuation changes significantly
**Purpose:** Track valuation changes for reporting

```java
{
  "companyId": 1,
  "inventoryType": "FINISHED_GOOD",
  "itemId": 123,
  "itemCode": "FG-001",
  "previousValue": 50000.00,
  "newValue": 42500.00,
  "changeAmount": -7500.00,
  "changeReason": "DISPATCH"
}
```
