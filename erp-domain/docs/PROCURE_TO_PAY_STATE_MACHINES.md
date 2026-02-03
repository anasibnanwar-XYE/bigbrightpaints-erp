# Procure-to-Pay State Machines and Invariants

This document captures the current P2P (purchasing + AP) flows and invariants
for traceability and ERP-grade correctness. It reflects existing behavior only.

## Purchase Order Statuses
Source: `PurchaseOrder`, `PurchasingService`.

States:
- `OPEN`: default state on creation.
- `PARTIAL`: one or more goods receipts are less than ordered quantities.
- `RECEIVED`: goods receipt quantities match the purchase order.
- `CLOSED`: a matching invoice has been posted for a fully received order.

## Goods Receipt Statuses
Source: `GoodsReceipt`, `PurchasingService`.

States:
- `RECEIVED`: default state on creation.
- `PARTIAL`: receipt quantity is less than the purchase order for at least one line.
- `INVOICED`: linked to a posted purchase invoice.

## Raw Material Purchase Statuses
Source: `RawMaterialPurchase`, `PurchasingService`.

States (string values stored on `RawMaterialPurchase.status`):
- `POSTED`: default status on creation (fully outstanding).
- `PARTIAL`: outstanding reduced but not zero.
- `PAID`: outstanding cleared to zero.

Transitions (current behavior):
- Create purchase -> `POSTED`, `outstandingAmount = totalAmount`.
- Supplier settlement reduces `outstandingAmount` by applied + discount + write-off + FX adjustment.
  - outstanding == 0 -> `PAID`
  - 0 < outstanding < total -> `PARTIAL`
  - outstanding == total -> `POSTED`

## Go-Live Limitations (current behavior)
- Thin PO scaffolding is implemented for workflow visibility; PO does not post journals.
- GRN (goods receipt) **does** record inventory:
  - raw material batches are created/updated
  - raw material movements are recorded (stock increases)
  - no GL journal is posted at GRN time (GL/AP post at supplier invoice time)
- Supplier invoices are still captured via `raw-material-purchases`, but now require a matching PO + GRN.
- `raw-materials/intake` and manual batch creation are adjustment-only paths. They are disabled by default
  (`erp.raw-material.intake.enabled=false`) and should not be used to bypass supplier invoices.

## Raw Material Movement Types
Source: `RawMaterialService`, `PurchasingService`.

Movements tied to purchases:
- `RECEIPT` with `referenceType = RAW_MATERIAL_PURCHASE` for intake.
- `RETURN` with `referenceType = PURCHASE_RETURN` for supplier returns.

## Supplier Settlements
Source: `AccountingService#settleSupplierInvoices`.

Flow:
- Settlements allocate amounts to purchases via `PartnerSettlementAllocation` rows.
- Journal entry lines:
  - Dr AP (supplier payable) for applied amount.
  - Cr cash/bank for actual cash paid.
  - Optional discount/write-off/FX lines as provided.
- Purchases linked in allocations have `outstandingAmount` reduced by the cleared amount.

## Supplier Payments vs Settlements (Two APIs)
Source: `AccountingController`, `AccountingService`.

Current surface area includes both:
- Supplier payments: `POST /api/v1/accounting/suppliers/payments`
- Supplier settlements: `POST /api/v1/accounting/settlements/suppliers`

Behavior (current):
- Both allocate to supplier purchases and reduce `outstandingAmount`.
- Settlements support non-cash adjustments (discount/write-off/FX) while payments are a simpler “cash-only” shape.

CODE-RED rule:
- Both paths must be idempotent under retries and must never double-apply allocations.

## Cross-Module Invariants
These invariants must hold for a canonical P2P flow:
- Purchase Order → Goods Receipt:
  - GRN raw material quantities must not exceed PO quantities.
- Goods Receipt → Purchase Invoice:
  - Invoice line quantities and unit costs must match the GRN.
- Purchase → raw material batch created and linked to each line.
- Purchase → raw material movement created per intake line (reference type `RAW_MATERIAL_PURCHASE`).
- Purchase journal:
  - linked to the purchase and to raw material movements (`journalEntryId`).
  - balanced (inventory debits + input tax = AP credit).
- Supplier settlement:
  - creates a balanced journal entry with AP + cash/discount/write-off/FX lines.
  - allocations link back to the supplier and purchase(s).
- Purchase returns:
  - create a `RETURN` movement and a purchase return journal (Dr AP / Cr inventory).
  - stock decreases by return quantity.
  - policy note: if returns exceed outstanding, the system can represent a supplier credit; this must be documented and
    enforced consistently (avoid silent negative-outstanding drift).

## Idempotency and Retry Safety
- Purchase invoices are unique per company (`invoice_number` constraint).
- Purchase journals use reference numbers for idempotent reuse.
- Supplier settlements use `idempotencyKey` to prevent duplicate allocations.

## GST / Tax Handling
- Purchase journals accept optional tax lines.
- Inventory + tax lines must equal the AP total; mismatches reject the post.
