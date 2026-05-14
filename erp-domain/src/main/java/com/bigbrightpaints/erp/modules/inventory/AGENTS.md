# Inventory Module

Last reviewed: 2026-03-29

## Overview

The inventory module owns stock visibility, reservations, adjustments, batch traceability, opening stock, and valuation. It is the stock truth boundary for finished goods and raw materials.

## What This Module Owns

- **Stock summaries** — current stock levels for finished goods and raw materials.
- **Batch traceability** — finished good batches and raw material batches with full movement history.
- **Reservations** — order-linked stock reservations via `FinishedGoodsReservationEngine`.
- **Dispatch execution** — slip-level dispatch decrements via `FinishedGoodsDispatchEngine`.
- **Adjustments** — raw material adjustments and stock corrections.
- **Opening stock** — batch-level opening stock import with idempotency.
- **Valuation** — stock valuation and costing method selection.

## Primary Controllers

- `OpeningStockImportController` — opening stock import.
- Inventory reads and writes are primarily accessed through sales and purchasing controllers.

## Key Services

- `FinishedGoodsReservationEngine` — allocates batches by costing method and records reservations.
- `FinishedGoodsDispatchEngine` — slip-level dispatch with stock decrement and backorder creation.
- `InventoryMovementRecorder` — records stock movements and publishes movement events.
- `OpeningStockImportService` — opening stock import with batch key idempotency.

## DTO Families

- `InventoryMovement` — stock movement records.
- `FinishedGoodBatch` / `RawMaterialBatch` — batch-level stock representations.
- `PackagingSlip` — dispatch slip representation.

## Cross-Module Boundaries

- **Sales → Inventory:** order reservation, dispatch execution, packaging slip management.
- **Purchasing → Inventory:** GRN raw material receipt, purchase return stock deduction.
- **Factory → Inventory:** raw material consumption, finished goods creation, batch registration.
- **Inventory -.events.→ Accounting:** `InventoryMovementEvent` → `InventoryAccountingEventListener`.

## Canonical Documentation

For the full architecture reference, see:
- [docs/ARCHITECTURE.md](../../../../../../../docs/ARCHITECTURE.md)
- [docs/INDEX.md](../../../../../../../docs/INDEX.md)

## Known Limitations

- Dispatch ownership is split across modules: inventory owns the transport/controller path while sales and accounting own commercial posting.
- Some stock operations depend on configuration toggles (e.g., `erp.inventory.accounting.events.enabled`).
