# Factory Module - Canonical Flows

This document describes the primary business workflows in the Factory module.

## 1. Manufacturing-to-Stock (M2S) Flow

The core production workflow that transforms raw materials into sellable finished goods.

### Flow Overview
```
┌─────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌───────────────┐
│ Raw         │    │ Production Log   │    │ Semi-Finished   │    │ Finished      │
│ Materials   │───▶│ (Mixing)         │───▶│ Bulk Batch      │───▶│ Goods         │
└─────────────┘    └──────────────────┘    └─────────────────┘    └───────────────┘
       │                    │                      │                      │
       ▼                    ▼                      ▼                      ▼
┌─────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌───────────────┐
│ Inventory   │    │ WIP Account      │    │ Valuation       │    │ Sales-Ready   │
│ Deduction   │    │ Journal          │    │ Account         │    │ Inventory     │
└─────────────┘    └──────────────────┘    └─────────────────┘    └───────────────┘
```

### Step 1: Production Log Creation

**Endpoint**: `POST /api/v1/factory/production/logs`

**Request Body**:
```json
{
  "brandId": 1,
  "productId": 100,
  "batchColour": "GLOSS WHITE",
  "batchSize": 500,
  "unitOfMeasure": "LITER",
  "mixedQuantity": 480,
  "producedAt": "2024-01-15T10:30:00Z",
  "createdBy": "john.doe",
  "laborCost": 150.00,
  "overheadCost": 75.00,
  "materials": [
    { "rawMaterialId": 10, "quantity": 200, "unitOfMeasure": "KG" },
    { "rawMaterialId": 11, "quantity": 150, "unitOfMeasure": "L" }
  ]
}
```

**Processing Steps**:
1. **Validation**
   - Verify brand belongs to company
   - Verify product belongs to brand
   - Validate all materials exist and have sufficient stock

2. **Material Consumption (FIFO)**
   - Lock raw material batches in FIFO order
   - Calculate weighted average cost per material
   - Deduct quantities atomically (prevents negative stock)
   - Create `RawMaterialMovement` records (type: ISSUE)
   - Link movements to production log reference

3. **Cost Calculation**
   ```
   totalCost = materialCostTotal + laborCost + overheadCost
   unitCost = totalCost / mixedQuantity
   ```

4. **Semi-Finished Batch Creation**
   - Create `FinishedGood` with SKU `{productSku}-BULK` (e.g., "SAFARI-WHITE-BULK")
   - Create `FinishedGoodBatch` with code = `productionCode`
   - Set batch as `bulk = true`
   - Create `InventoryMovement` (type: RECEIPT)

5. **Journal Entries**
   - **Material Journal**:
     - Debit: WIP Account
     - Credit: Raw Material Inventory Accounts
   - **Labor/Overhead Journal**:
     - Debit: WIP Account
     - Credit: Labor Applied / Overhead Applied

**Response**: `ProductionLogDetailDto` with generated `productionCode` (e.g., "PROD-20240115-001")

---

### Step 2: Packing Operation

**Endpoint**: `POST /api/v1/factory/packing-records`

**Headers**:
```
Idempotency-Key: <unique-key>
```

**Request Body**:
```json
{
  "productionLogId": 123,
  "packedDate": "2024-01-15",
  "packedBy": "jane.smith",
  "lines": [
    {
      "childFinishedGoodId": 201,
      "packagingSize": "1L",
      "quantityLiters": 100,
      "piecesCount": 100,
      "piecesPerBox": 12
    },
    {
      "childFinishedGoodId": 202,
      "packagingSize": "5L",
      "quantityLiters": 200,
      "piecesCount": 40,
      "piecesPerBox": 4
    }
  ],
  "closeResidualWastage": false
}
```

**Processing Steps**:

1. **Idempotency Check**
   - Reserve `PackingRequestRecord` with key
   - If exists with same hash: return existing result
   - If exists with different log: error
   - If exists with different hash: error (payload mismatch)

2. **For Each Packing Line**:
   a. **Resolve Target**
      - Validate `childFinishedGoodId` is in allowed sellable sizes
      - Validate `packagingSize` matches size variant

   b. **Consume Packaging Material**
      - Look up `PackagingSizeMapping` for size (e.g., "1L")
      - Deduct packaging material (buckets) from inventory
      - Create `RawMaterialMovement` records

   c. **Consume Semi-Finished Inventory**
      - Lock semi-finished batch
      - Deduct quantity from bulk batch
      - Create `InventoryMovement` (type: ISSUE)

   d. **Register Finished Good Batch**
      - Create `FinishedGoodBatch` for sellable size
      - Calculate unit cost: `baseUnitCost + packagingCostPerUnit`
      - Create `InventoryMovement` (type: RECEIPT)

   e. **Post Journal Entry**
      - Debit: Finished Good Valuation Account
      - Credit: Semi-Finished Account (base cost)
      - Credit: WIP Account (packaging cost)

3. **Update Production Log**
   - Increment `totalPackedQuantity`
   - Update status: `READY_TO_PACK` → `PARTIAL_PACKED` → `FULLY_PACKED`
   - If `closeResidualWastage`: consume remaining as wastage

**Response**: Updated `ProductionLogDetailDto`

---

### Step 3: Close with Wastage (Optional)

**Request**:
```json
{
  "productionLogId": 123,
  "lines": [],
  "closeResidualWastage": true
}
```

**Processing**:
- Calculate residual: `mixedQuantity - totalPackedQuantity`
- Consume residual from semi-finished batch
- Post wastage journal:
  - Debit: Wastage Account
  - Credit: Semi-Finished Account
- Set status to `FULLY_PACKED`
- Set `wastageQuantity` and `wastageReasonCode`

---

## 2. Bulk-to-Size Packing Flow

Converts a bulk finished goods batch into smaller sized SKUs.

### Use Case
A bulk batch of "SAFARI-WHITE-BULK" (1000L) needs to be repacked into:
- Safari-WHITE-1L (200 units)
- Safari-WHITE-4L (50 units)
- Safari-WHITE-20L (20 units)

### Flow Overview
```
┌─────────────────┐    ┌───────────────────┐    ┌─────────────────────┐
│ Bulk FG Batch   │    │ Bulk Pack         │    │ Child FG Batches    │
│ (Parent)        │───▶│ Operation         │───▶│ (1L, 4L, 20L)       │
└─────────────────┘    └───────────────────┘    └─────────────────────┘
        │                       │                         │
        ▼                       ▼                         ▼
┌─────────────────┐    ┌───────────────────┐    ┌─────────────────────┐
│ Stock Reduced   │    │ Packaging         │    │ Stock Added         │
│ (1000L → 0L)    │    │ Consumed          │    │ Ready for Sale      │
└─────────────────┘    └───────────────────┘    └─────────────────────┘
```

### API Call

**Endpoint**: `POST /api/v1/factory/bulk-batches/pack` (via BulkPackingService.pack)

**Request Body**:
```json
{
  "bulkBatchId": 456,
  "packDate": "2024-01-20",
  "packedBy": "john.packer",
  "notes": "Repack bulk to retail sizes",
  "packs": [
    { "childSkuId": 201, "quantity": 200, "sizeLabel": "1L" },
    { "childSkuId": 202, "quantity": 50, "sizeLabel": "4L" },
    { "childSkuId": 203, "quantity": 20, "sizeLabel": "20L" }
  ]
}
```

### Processing Steps

1. **Validation**
   - Verify bulk batch exists and belongs to company
   - Verify batch is marked as `bulk = true`
   - Verify no duplicate child SKU lines
   - Calculate total volume: `sum(quantity × sizeInLiters)`
   - Verify sufficient stock: `totalVolume ≤ bulkBatch.quantityAvailable`

2. **Idempotency Check**
   - Generate reference: `PACK-{batchCode}-{hash}`
   - Check for existing movements with reference
   - Return existing result if found

3. **Packaging Consumption**
   - For each pack line, consume packaging materials
   - Create `RawMaterialMovement` records

4. **Child Batch Creation**
   - For each pack line:
     - Calculate unit cost: `bulkUnitCost × sizeInLiters + packagingCostPerUnit`
     - Create `FinishedGoodBatch` with parent reference
     - Create `InventoryMovement` (type: RECEIPT)

5. **Bulk Inventory Deduction**
   - Reduce bulk batch quantities
   - Create `InventoryMovement` (type: ISSUE)

6. **Journal Entry**
   - Debit: Child FG Valuation Accounts
   - Credit: Bulk FG Valuation Account
   - Credit: Packaging Material Accounts

**Response**: `BulkPackResponse` with child batch details

---

## 3. Period Cost Allocation Flow

Distributes actual labor and overhead costs to production batches for a period.

### Use Case
At month-end, actual labor costs of $15,000 and overhead of $8,000 need to be allocated to batches that already have estimated costs.

### API Call

**Endpoint**: `POST /api/v1/factory/cost-allocation`

**Request Body**:
```json
{
  "year": 2024,
  "month": 1,
  "laborCost": 15000.00,
  "overheadCost": 8000.00,
  "finishedGoodsAccountId": 1001,
  "laborExpenseAccountId": 5001,
  "overheadExpenseAccountId": 5002,
  "notes": "January 2024 actual cost allocation"
}
```

### Processing Steps

1. **Find Eligible Batches**
   - Query fully packed batches for the month
   - Skip batches with existing cost variance journals

2. **Calculate Totals**
   - Sum total liters produced
   - Sum already-applied labor and overhead

3. **Calculate Variance**
   ```
   laborVariance = actualLaborCost - appliedLaborTotal
   overheadVariance = actualOverheadCost - appliedOverheadTotal
   totalVariance = laborVariance + overheadVariance
   ```

4. **Allocate to Batches**
   - For each batch (proportional to liters):
     - Calculate batch labor variance
     - Calculate batch overhead variance
     - Update `laborCostTotal` and `overheadCostTotal`
     - Recalculate `unitCost`
     - Update child batch unit costs

5. **Post Journals**
   - For each batch with variance:
     - Debit: Finished Goods Account
     - Credit: Labor Expense Account
     - Credit: Overhead Expense Account

**Response**: `CostAllocationResponse` with summary

---

## 4. Packaging Rule Configuration

### Create Packaging Mapping

**Endpoint**: `POST /api/v1/factory/packaging-mappings`

**Request Body**:
```json
{
  "packagingSize": "5L",
  "rawMaterialId": 50,
  "unitsPerPack": 1,
  "cartonSize": 4,
  "litersPerUnit": 5
}
```

**Purpose**: When packing 5L containers, automatically consume 1 unit of raw material #50 (5L bucket) per piece.

### Active Rules Required
Packing operations will fail if no active packaging mapping exists for the requested size.

---

## State Transitions

### ProductionLog Status
```
MIXED ──▶ READY_TO_PACK ──▶ PARTIAL_PACKED ──▶ FULLY_PACKED
   │                              │
   └──────────────────────────────┘
```

### ProductionPlan Status
```
PLANNED ──▶ IN_PROGRESS ──▶ COMPLETED
```

### FactoryTask Status
```
PENDING ──▶ IN_PROGRESS ──▶ COMPLETED
```

---

## Error Handling

### Common Error Codes

| Code | Description |
|------|-------------|
| `VALIDATION_INVALID_INPUT` | Invalid request data |
| `VALIDATION_INVALID_REFERENCE` | Referenced entity not found |
| `VALIDATION_MISSING_REQUIRED_FIELD` | Required field missing |
| `CONCURRENCY_CONFLICT` | Idempotency or optimistic lock conflict |
| `BUSINESS_ENTITY_NOT_FOUND` | Entity not found |
| `BUSINESS_CONSTRAINT_VIOLATION` | Business rule violation |

### Packing-Specific Errors

| Scenario | Error |
|----------|-------|
| Production log already fully packed | "Production log X is already fully packed" |
| Insufficient raw material stock | "Insufficient X. Need: Y, Available: Z" |
| Missing packaging setup | "Packaging Setup is required for size X" |
| Invalid sellable size target | "Sellable size target is not allowed" |
| Packed quantity exceeds mixed | "Packed quantity would exceed mixed quantity" |

---

## Idempotency Guarantees

### Packing Operations
- Required `Idempotency-Key` header
- Hash includes: log ID, date, packed by, all lines
- Same key + same payload = returns existing result
- Same key + different log = error
- Same key + different payload = error

### Bulk Packing
- Automatic key derived from: batch ID, pack lines, optional custom key
- Returns existing result if same operation exists

### Production Plans & Tasks
- Natural key idempotency via `planNumber` or `salesOrderId + title`
- Same payload = returns existing
- Different payload = error

---

## Related Documentation
- [CONTROLLERS.md](./CONTROLLERS.md) - API endpoints
- [SERVICES.md](./SERVICES.md) - Service layer details
- [ENTITIES.md](./ENTITIES.md) - Domain model
- [DTOS.md](./DTOS.md) - Request/response structures
