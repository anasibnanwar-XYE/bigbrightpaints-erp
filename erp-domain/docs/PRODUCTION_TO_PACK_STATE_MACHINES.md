# Production-to-Pack State Machines and Invariants

This document captures the current factory production + packing flows and
invariants for traceability and ERP-grade correctness. It reflects existing
behavior only; no new flows are introduced.

Primary reference docs:
- Module overview: `erp-domain/docs/MODULE_FLOW_MAP.md`
- Cross-module linkage expectations: `erp-domain/docs/CROSS_MODULE_LINKAGE_MATRIX.md`
- CODE-RED target packaging model (Plan B): `docs/CODE-RED/packaging-flow.md`

## Concepts (Current Behavior)

- **Production log**: records a bulk production run, including raw material issues and WIP postings.
- **Packing record**: converts semi-finished/bulk into sellable finished goods, consuming packaging materials and
  creating finished good batches.
- **Bulk packing**: converts an existing bulk batch into size SKUs (child batches) with associated movements and
  conversion journals.

## Production Log (Bulk Production)
Source: `ProductionLogService`.

Current behavior (high-level):
1) Consume raw materials (FIFO where configured) and record raw material movements.
2) Post WIP journals (materials + labor/overhead) and link to movements where applicable.
3) Create a bulk/semi-finished finished-good batch (often referred to as `SKU-BULK` behavior) and record inventory
   receipt movements.

Key invariants:
- Raw material consumption must not over-consume available batches.
- Business date handling uses company time (never server timezone).
- Journals must balance and must be traceable from the production reference.

## Packing Records (Semi-Finished → Finished Goods)
Source: `PackingService`, `PackagingMaterialService`.

Current behavior (high-level):
1) Consume packaging raw materials (record raw material movements).
2) Consume semi-finished inventory and create finished-good batches (record finished-good movements).
3) Post conversion journals (including packaging material cost) and link movements where implemented.

Known CODE-RED risk (to harden, without changing flows):
- Retries can generate new movement/journal references (for example, movement-id-derived references), which can create duplicates.
  Stabilization work focuses on request-level idempotency + deterministic references.

## Bulk Packing (Bulk Batch → Size SKU Child Batches)
Source: `BulkPackingService`.

Current behavior (high-level):
1) Select a bulk finished-good batch.
2) For each requested size SKU:
   - issue bulk quantity
   - consume packaging RMs
   - create child batches and receipt movements
3) Post conversion journals and link movements where applicable.

Key invariants:
- Bulk batch identity is preserved via parent/child batch linkage.
- Consumption must be FIFO where configured and must not over-consume.

## Linkage Keys (Cross-Module Traceability)

Observed linkage patterns (see `erp-domain/docs/CROSS_MODULE_LINKAGE_MATRIX.md` for full key list):
- `inventory_movements.reference_type=PRODUCTION_LOG` uses `reference_id = production_logs.production_code`
- `raw_material_movements.reference_type=PACKING_RECORD` uses `reference_id = <production_code>-PACK-<packingRecordId>`
- `inventory_movements.reference_type=PACKING_RECORD` uses `reference_id = <packReference>` (bulk pack)
- `finished_good_batches.parent_batch_id` links bulk-to-size child batches (bulk packing)

Reference ID formats (current behavior)
- Packing record referenceId: `<productionCode>-PACK-<packingRecordId>`
- Packing WIP→FG journal reference: `<productionCode>-PACK-<inventoryMovementId>`
- Bulk packing referenceId: `PACK-<bulkBatchCode>-<hash>` (deterministic)
- Packaging material consumption journal reference:
  - packing: `<productionCode>-PACK-<packingRecordId>-PACKMAT`
  - bulk pack: `<packReference>-PACKMAT`

## Production Log Statuses
Source: `ProductionLogStatus`.

States:
- `MIXED` → `READY_TO_PACK` → `PARTIAL_PACKED` → `FULLY_PACKED`

Invariant:
- `FULLY_PACKED` is the only “packing complete” terminal state.

## Posting Boundary (Policy)

CODE-RED policy is that posting must be owned by accounting (facade/policy) and be idempotent under retries.
If any manufacturing path posts directly and bypasses the canonical posting policy, treat it as a stabilization target.
