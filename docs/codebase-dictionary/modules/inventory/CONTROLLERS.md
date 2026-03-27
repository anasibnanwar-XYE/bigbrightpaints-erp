# Inventory Module - Controllers

## Overview

The Inventory module exposes 6 REST controllers handling finished goods, raw materials, dispatch operations, batch traceability, adjustments, and opening stock imports.

---

## FinishedGoodController

**Path:** `modules/inventory/controller/FinishedGoodController.java`
**Base URL:** `/api/v1/finished-goods`
**Purpose:** Manages finished goods inventory, batches, and stock summaries

### Endpoints

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/` | ADMIN, FACTORY, SALES, ACCOUNTING | List all finished goods |
| GET | `/{id}` | ADMIN, FACTORY, SALES, ACCOUNTING | Get finished good by ID |
| GET | `/{id}/batches` | ADMIN, FACTORY, SALES | List batches for finished good |
| POST | `/{id}/batches` | ADMIN, FACTORY | Register new batch |
| GET | `/stock-summary` | ADMIN, FACTORY, SALES, ACCOUNTING | Get stock summary |
| GET | `/low-stock` | ADMIN, FACTORY, SALES | Get low stock items |
| GET | `/{id}/low-stock-threshold` | ADMIN, FACTORY, SALES, ACCOUNTING | Get low stock threshold |
| PUT | `/{id}/low-stock-threshold` | ADMIN, FACTORY, ACCOUNTING | Update low stock threshold |

### Dependencies
- `FinishedGoodsService` - Core facade for finished goods operations

### Request/Response DTOs
- `FinishedGoodDto` - Finished good response
- `FinishedGoodBatchDto` - Batch response
- `FinishedGoodBatchRequest` - Batch creation request
- `StockSummaryDto` - Stock summary response
- `FinishedGoodLowStockThresholdDto` - Threshold response
- `FinishedGoodLowStockThresholdRequest` - Threshold update request

---

## RawMaterialController

**Path:** `modules/inventory/controller/RawMaterialController.java`
**Base URL:** `/api/v1`
**Purpose:** Manages raw material inventory, stock adjustments, and expiring batches

### Endpoints

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/raw-materials/stock` | ADMIN, ACCOUNTING, FACTORY | Raw material stock summary |
| GET | `/raw-materials/stock/inventory` | ADMIN, ACCOUNTING, FACTORY | List inventory snapshots |
| GET | `/raw-materials/stock/low-stock` | ADMIN, ACCOUNTING, FACTORY | List low stock items |
| POST | `/inventory/raw-materials/adjustments` | ADMIN, ACCOUNTING | Adjust raw material stock |
| GET | `/inventory/batches/expiring-soon` | ADMIN, ACCOUNTING, FACTORY, SALES | List expiring batches |

### Idempotency
- Requires `Idempotency-Key` or `X-Idempotency-Key` header for adjustments
- Validates request body before processing

### Dependencies
- `RawMaterialService` - Core service for raw materials
- `InventoryBatchQueryService` - Query service for batch data
- `Validator` - Bean validation

### Request/Response DTOs
- `StockSummaryDto` - Stock summary
- `InventoryStockSnapshot` - Inventory snapshot
- `RawMaterialAdjustmentDto` - Adjustment response
- `RawMaterialAdjustmentRequest` - Adjustment request
- `InventoryExpiringBatchDto` - Expiring batch info

---

## InventoryBatchController

**Path:** `modules/inventory/controller/InventoryBatchController.java`
**Base URL:** `/api/v1/inventory/batches`
**Purpose:** Batch traceability and movement history

### Endpoints

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/{id}/movements` | ADMIN, FACTORY, ACCOUNTING, SALES | Get batch movement history |

### Query Parameters
- `batchType` (optional): RAW_MATERIAL, FINISHED_GOOD, or auto-detected

### Dependencies
- `InventoryBatchTraceabilityService` - Traceability service

### Request/Response DTOs
- `InventoryBatchTraceabilityDto` - Full traceability response with movements

---

## DispatchController

**Path:** `modules/inventory/controller/DispatchController.java`
**Base URL:** `/api/v1/dispatch`
**Purpose:** Read-only dispatch operations for factory-facing workflows

### Endpoints

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/pending` | ADMIN, FACTORY, SALES | Get pending packaging slips |
| GET | `/preview/{slipId}` | ADMIN, FACTORY | Get dispatch preview for confirmation |
| GET | `/slip/{slipId}` | ADMIN, FACTORY, SALES | Get packaging slip details |
| GET | `/order/{orderId}` | ADMIN, FACTORY, SALES | Get packaging slip by sales order |
| GET | `/slip/{slipId}/challan/pdf` | ADMIN, FACTORY | Download delivery challan PDF |

### Cost Redaction
Factory operational views (non-elevated roles) automatically redact unit cost data from responses.

### Dependencies
- `FinishedGoodsService` - Core service
- `DeliveryChallanPdfService` - PDF generation

### Request/Response DTOs
- `PackagingSlipDto` - Packaging slip response
- `DispatchPreviewDto` - Dispatch preview with GST breakdown

---

## InventoryAdjustmentController

**Path:** `modules/inventory/controller/InventoryAdjustmentController.java`
**Base URL:** `/api/v1/inventory/adjustments`
**Purpose:** Finished goods inventory adjustments (damage, recount, etc.)

### Endpoints

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/` | ADMIN, ACCOUNTING | List all adjustments |
| POST | `/` | ADMIN, ACCOUNTING | Create adjustment |

### Idempotency
- Requires `Idempotency-Key` or `X-Idempotency-Key` header
- Validates adjustment request before processing

### Dependencies
- `InventoryAdjustmentService` - Core adjustment service
- `Validator` - Bean validation

### Request/Response DTOs
- `InventoryAdjustmentDto` - Adjustment response
- `InventoryAdjustmentRequest` - Adjustment request with lines

---

## OpeningStockImportController

**Path:** `modules/inventory/controller/OpeningStockImportController.java`
**Base URL:** `/api/v1/inventory`
**Purpose:** Opening stock CSV imports for migration/onboarding

### Endpoints

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/opening-stock` | ADMIN, ACCOUNTING, FACTORY | Import opening stock CSV |
| GET | `/opening-stock` | ADMIN, ACCOUNTING, FACTORY | List import history |

### Multipart Upload
- Accepts `multipart/form-data` with file part
- Requires `Idempotency-Key` header
- Requires `openingStockBatchKey` parameter

### Response Sanitization
Non-accounting users receive sanitized responses with accounting metadata redacted.

### Dependencies
- `OpeningStockImportService` - Import service
- `SkuReadinessService` - SKU readiness validation

### Request/Response DTOs
- `OpeningStockImportResponse` - Import result with row results and errors
- `OpeningStockImportHistoryItem` - History item for listing

---

## Common Patterns

### Idempotency
All write operations require idempotency keys to prevent duplicate processing:
- Header: `Idempotency-Key` (preferred) or `X-Idempotency-Key` (legacy)
- Request body: `idempotencyKey` field (takes precedence)

### Role-Based Access
- **ADMIN**: Full access to all operations
- **ACCOUNTING**: Financial operations, adjustments
- **FACTORY**: Operational views, batch registration, dispatch
- **SALES**: Read access to finished goods, dispatch status

### Error Handling
All endpoints return `ApiResponse<T>` wrapper with:
- `success`: boolean
- `message`: operation description
- `data`: response payload
- `error`: error details (on failure)
