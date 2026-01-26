# Manufacturing Portal Sitemap & Data Map

Scope: manufacturing-facing UI aligned to current backend endpoints. This map is intentionally UI-first and backend-backed; any page without an endpoint is marked as “internal/derived.”

## Sitemap (Page/Navigation Tree)

- Dashboard
  - KPIs: production WIP, packed today, dispatch pending, stock low
  - Widgets: Production queue, Packing queue, Dispatch queue, Inventory alerts
  - Data: `/api/v1/factory/dashboard`, `/api/v1/dispatch/pending`, `/api/v1/finished-goods/stock-summary`, `/api/v1/finished-goods/low-stock`

- Production
  - Production Plans (list/detail)
    - List/Create/Update
    - Data: `/api/v1/factory/production-plans` (GET/POST/PUT/PATCH)
  - Production Batches (list/detail)
    - Create from plan, view batch details
    - Data: `/api/v1/factory/production-batches` (GET/POST)
  - Production Tasks (list/detail)
    - Assign/Update tasks for batches
    - Data: `/api/v1/factory/tasks` (GET/POST/PUT)
  - Production Logs (list/detail)
    - Log actuals, yields
    - Data: `/api/v1/factory/production/logs` (GET/POST), `/api/v1/factory/production/logs/{id}`
  - Cost Allocation
    - Apply overheads/cost rules
    - Data: `/api/v1/factory/cost-allocation` (POST)

- Packaging
  - Packing Records (create/complete)
    - Capture pack output per production log
    - Data: `/api/v1/factory/packing-records` (POST), `/api/v1/factory/packing-records/{productionLogId}/complete` (POST)
  - Unpacked Batches
    - Queue of bulk to pack
    - Data: `/api/v1/factory/unpacked-batches` (GET)
  - Pack (Bulk → Size)
    - Packaging operation view
    - Data: `/api/v1/factory/pack` (POST)
  - Packaging Mappings
    - Define packaging conversion rules
    - Data: `/api/v1/factory/packaging-mappings` (GET/POST), `/api/v1/factory/packaging-mappings/{id}` (PUT/DELETE)
  - Packing History
    - Per production log history
    - Data: `/api/v1/factory/production-logs/{productionLogId}/packing-history` (GET)

- Dispatch
  - Dispatch Queue
    - Pending slips list
    - Data: `/api/v1/dispatch/pending` (GET)
  - Dispatch Slip Detail
    - View slip, order linkage, item lines
    - Data: `/api/v1/dispatch/slip/{slipId}` (GET), `/api/v1/dispatch/order/{orderId}` (GET)
  - Dispatch Preview
    - Preview before confirmation
    - Data: `/api/v1/dispatch/preview/{slipId}` (GET)
  - Dispatch Confirm
    - Confirm dispatch; inventory decrement
    - Data: `/api/v1/dispatch/confirm` (POST)
  - Backorder / Cancel
    - Cancel backorder
    - Data: `/api/v1/dispatch/backorder/{slipId}/cancel` (POST)
  - Slip Status Update
    - Change slip status
    - Data: `/api/v1/dispatch/slip/{slipId}/status` (PATCH)

- Inventory
  - Finished Goods Catalog
    - List, create, update
    - Data: `/api/v1/finished-goods` (GET/POST), `/api/v1/finished-goods/{id}` (GET/PUT)
  - Finished Goods Batches
    - Batch list + create
    - Data: `/api/v1/finished-goods/{id}/batches` (GET/POST)
  - Bulk Batch Explorer
    - Bulk batches and child batches
    - Data: `/api/v1/factory/bulk-batches/{finishedGoodId}` (GET), `/api/v1/factory/bulk-batches/{parentBatchId}/children` (GET)
  - Stock Summary
    - Data: `/api/v1/finished-goods/stock-summary` (GET)
  - Low Stock
    - Data: `/api/v1/finished-goods/low-stock` (GET)
  - Inventory Adjustments (Out)
    - Record shrinkage/damaged/obsolete
    - Data: `/api/v1/inventory/adjustments` (GET/POST)

- Raw Materials
  - Raw Materials Catalog
    - Data: `/api/v1/accounting/raw-materials` (GET/POST/PUT/DELETE)
  - Raw Material Stock
    - Data: `/api/v1/raw-materials/stock` (GET), `/api/v1/raw-materials/stock/inventory` (GET), `/api/v1/raw-materials/stock/low-stock` (GET)
  - Raw Material Batches
    - Data: `/api/v1/raw-material-batches/{rawMaterialId}` (GET/POST)
  - Raw Material Intake
    - Data: `/api/v1/raw-materials/intake` (POST)

- Documents (Read-Only for Manufacturing)
  - Invoices by Dealer (view only)
    - Data: `/api/v1/invoices/dealers/{dealerId}` (GET)
  - Invoice Detail / PDF (view only)
    - Data: `/api/v1/invoices/{id}` (GET), `/api/v1/invoices/{id}/pdf` (GET)

- Utilities
  - Lookups / Master Data (derived views)
  - Audit / Activity Log (derived)

## Data Map (Entities, Ownership, and API Anchors)

Each entity lists its primary owner module, key fields to surface in UI, and API anchors.

- ProductionPlan
  - Owner: Factory
  - Key fields: id, code, status, targetQty, startDate, endDate, product(s)
  - API: `GET/POST/PUT/PATCH /api/v1/factory/production-plans`

- ProductionBatch
  - Owner: Factory
  - Key fields: id, planId, batchNo, status, scheduledQty, actualQty, startedAt, finishedAt
  - API: `GET/POST /api/v1/factory/production-batches`

- Task
  - Owner: Factory
  - Key fields: id, batchId, station, assignee, status, eta
  - API: `GET/POST/PUT /api/v1/factory/tasks`

- ProductionLog
  - Owner: Factory
  - Key fields: id, batchId, yieldQty, wasteQty, notes, createdAt
  - API: `GET/POST /api/v1/factory/production/logs`, `GET /api/v1/factory/production/logs/{id}`

- PackingRecord
  - Owner: Factory
  - Key fields: id, productionLogId, sku, qtyPacked, status
  - API: `POST /api/v1/factory/packing-records`, `POST /api/v1/factory/packing-records/{productionLogId}/complete`

- PackagingMapping
  - Owner: Factory
  - Key fields: id, bulkSku, packSku, conversionRate, active
  - API: `GET/POST /api/v1/factory/packaging-mappings`, `PUT/DELETE /api/v1/factory/packaging-mappings/{id}`

- FinishedGood
  - Owner: Inventory
  - Key fields: id, sku, name, uom, reorderPoint, defaultBatchSize
  - API: `GET/POST /api/v1/finished-goods`, `GET/PUT /api/v1/finished-goods/{id}`

- FinishedGoodBatch
  - Owner: Inventory
  - Key fields: id, finishedGoodId, batchNo, qtyOnHand, status
  - API: `GET/POST /api/v1/finished-goods/{id}/batches`

- DispatchSlip
  - Owner: Inventory/Dispatch
  - Key fields: id, orderId, status, lines[], requiredQty, reservedQty, dispatchedQty
  - API: `GET /api/v1/dispatch/slip/{slipId}`, `PATCH /api/v1/dispatch/slip/{slipId}/status`

- DispatchConfirmation
  - Owner: Inventory/Dispatch
  - Key fields: slipId, lines (batchId, qty), confirmedAt
  - API: `POST /api/v1/dispatch/confirm`, `GET /api/v1/dispatch/preview/{slipId}`

- InventoryAdjustment (Out)
  - Owner: Inventory
  - Key fields: id, finishedGoodId, qty, reason, createdAt
  - API: `GET/POST /api/v1/inventory/adjustments`

- RawMaterial
  - Owner: Materials
  - Key fields: id, code, name, uom, reorderPoint
  - API: `GET/POST/PUT/DELETE /api/v1/accounting/raw-materials`

- RawMaterialBatch
  - Owner: Materials
  - Key fields: id, rawMaterialId, batchNo, qtyOnHand
  - API: `GET/POST /api/v1/raw-material-batches/{rawMaterialId}`

- RawMaterialIntake
  - Owner: Materials
  - Key fields: rawMaterialId, qtyReceived, supplierId, receivedAt
  - API: `POST /api/v1/raw-materials/intake`

- Invoice (Read-only)
  - Owner: Sales/Accounting
  - Key fields: id, dealerId, status, total, tax, discount
  - API: `GET /api/v1/invoices/{id}`, `GET /api/v1/invoices/dealers/{dealerId}`, `GET /api/v1/invoices/{id}/pdf`

## Cross-Module Data Flow (Manufacturing View)

- Production Plan → Production Batch → Production Log
  - Source: Factory endpoints
  - Output: Packing records and finished good batches

- Packing Record → Finished Good Batches
  - Source: Packing endpoints
  - Inventory view shows new batches and updated stock

- Sales Order → Dispatch Slip → Dispatch Confirm
  - Source: Dispatch endpoints
  - Confirm reduces finished-good stock; links to invoices

- Inventory Adjustment (Out)
  - Source: Inventory adjustment endpoint
  - Decreases stock for shrinkage/damage/obsolete

## Required UI Data Joins (Client-side/Service layer)

- Dispatch Slip + FinishedGood + Batch
  - Needed for batch-level picks and available stock display

- Production Log + Packing History
  - Needed to show packed vs remaining per batch

- FinishedGood + Stock Summary + Low Stock
  - Needed for alerts and replenishment signals

## Permissions (Portal Default)

- Manufacturing roles: read/write on Production, Packaging, Dispatch, Inventory, Raw Materials
- Read-only on Invoices

