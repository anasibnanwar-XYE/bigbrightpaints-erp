# Inventory Module Overview

## Module Summary

The Inventory module manages raw materials, finished goods, batch tracking, stock movements, reservations, and dispatch operations for BigBright ERP. It supports paint manufacturing workflows including bulk-to-size packaging, multi-location stock, and GST-compliant inventory accounting.

| Attribute | Value |
|-----------|-------|
| Package | `com.bigbrightpaints.erp.modules.inventory` |
| Files | 71 Java files |
| Controllers | 6 |
| Services | 12 |
| Repositories | 13 |
| Entities | 15 |
| DTOs | 23 |
| Events | 2 |

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CONTROLLERS                                     │
│  RawMaterialController  │  FinishedGoodController  │  DispatchController   │
│  InventoryAdjustmentController  │  InventoryBatchController                 │
│  OpeningStockImportController                                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               SERVICES                                       │
│  ┌─────────────────── FACADE ───────────────────┐                           │
│  │           FinishedGoodsService                │                           │
│  └───────────────────────────────────────────────┘                           │
│                       │                                                       │
│  ┌────────────────── WORKFLOW ENGINE ───────────┐                           │
│  │      FinishedGoodsWorkflowEngineService       │                           │
│  └───────────────────────────────────────────────┘                           │
│                       │                                                       │
│  ┌──────────────────── SPECIALIZED ─────────────────────┐                    │
│  │ FinishedGoodsReservationEngine │ FinishedGoodsDispatchEngine│            │
│  │ InventoryValuationService │ BatchNumberService │ RawMaterialService       │
│  │ InventoryAdjustmentService │ InventoryBatchTraceabilityService            │
│  │ OpeningStockImportService │ PackagingSlipService                         │
│  │ InventoryMovementRecorder │ DeliveryChallanPdfService                    │
│  └────────────────────────────────────────────────────┘                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              REPOSITORIES                                    │
│  FinishedGoodRepository │ RawMaterialRepository │ FinishedGoodBatchRepository│
│  RawMaterialBatchRepository │ InventoryMovementRepository                   │
│  RawMaterialMovementRepository │ PackagingSlipRepository                    │
│  InventoryReservationRepository │ InventoryAdjustmentRepository             │
│  RawMaterialAdjustmentRepository │ OpeningStockImportRepository             │
│  RawMaterialIntakeRepository │ PackagingSlipLineRepository                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               ENTITIES                                       │
│  FinishedGood │ RawMaterial │ FinishedGoodBatch │ RawMaterialBatch          │
│  InventoryMovement │ RawMaterialMovement │ PackagingSlip │ PackagingSlipLine│
│  InventoryReservation │ InventoryAdjustment │ InventoryAdjustmentLine       │
│  RawMaterialAdjustment │ RawMaterialAdjustmentLine │ OpeningStockImport     │
│  RawMaterialIntakeRecord                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Key Responsibilities

1. **Raw Material Management**: SKU-based tracking, batch management, stock levels, costing methods (FIFO/LIFO/WAC)

2. **Finished Goods Management**: Product catalog, batch registration, low stock thresholds, GST configuration

3. **Batch Traceability**: Full movement history, source tracking (production/purchase/adjustment), expiry management

4. **Inventory Reservations**: Sales order reservations, batch-level allocation, backorder handling

5. **Dispatch Operations**: Packaging slips, delivery challans, logistics tracking, COGS posting

6. **Stock Adjustments**: Recount (up/down), damaged stock, inventory valuation, journal posting

7. **Opening Stock Import**: CSV-based bulk import, SKU readiness validation, opening balance posting

## Module Dependencies

| Dependency | Purpose |
|------------|---------|
| accounting | Journal entries, COGS posting, costing methods |
| company | Multi-tenant context, company defaults |
| sales | Sales order integration, dealer info |
| purchasing | Supplier references, raw material purchases |
| production | SKU readiness, product catalog sync |

## Canonicality Status

| Component | Status | Notes |
|-----------|--------|-------|
| FinishedGoodsService | Canonical | Primary facade for finished goods operations |
| RawMaterialService | Canonical | Primary service for raw material operations |
| InventoryBatchTraceabilityService | Canonical | Single source for batch movement history |
| FinishedGoodsDispatchEngine | Canonical | Handles dispatch workflow |
| FinishedGoodsReservationEngine | Canonical | Handles reservation logic |
| OpeningStockImportService | Scoped | Only during initial data migration |

## Configuration Flags

| Flag | Default | Purpose |
|------|---------|---------|
| `erp.inventory.finished-goods.batch.enabled` | false | Enable manual batch registration |
| `erp.inventory.opening-stock.enabled` | false | Enable opening stock imports |
| `erp.raw-material.intake.enabled` | false | Enable manual raw material intake |

## Security

All controllers require role-based access:
- `ROLE_ADMIN`, `ROLE_ACCOUNTING`, `ROLE_FACTORY`, `ROLE_SALES` (varies by endpoint)
- Factory operational views redact cost information
- Idempotency keys required for all write operations
