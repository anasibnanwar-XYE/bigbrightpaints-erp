# Idempotency & Uniqueness Inventory (Task 00 — EPIC B1)

Scope: sales → inventory → invoice → accounting/settlement. Evidence links are file paths.

## Sales Orders
- Code: `SalesService.createOrder(...)` checks `SalesOrderRequest.resolveIdempotencyKey()` and loads by key  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`
- DB columns/indexes:
  - `sales_orders.idempotency_key` + unique index scoped by company  
    `erp-domain/src/main/resources/db/migration/V55__sales_order_idempotency.sql`
  - `sales_orders.idempotency_hash` (hash column, no unique constraint)  
    `erp-domain/src/main/resources/db/migration/V104__idempotency_hash_columns.sql`
  - Idempotency markers for postings: `sales_journal_entry_id`, `cogs_journal_entry_id`, `fulfillment_invoice_id`  
    `erp-domain/src/main/resources/db/migration/V75__sales_order_idempotency_markers.sql`
  - Unique order number per company  
    `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesOrder.java`

## Dispatch / Packaging Slips
- Slip identity: unique slip number per company  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/PackagingSlip.java`
- Slip ↔ invoice unique link  
  `erp-domain/src/main/resources/db/migration/V90__packaging_slip_invoice_unique.sql`
- Dispatch flow uses slip status + order markers to avoid double posting  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`

## Journals (AR/COGS/Sales Return)
- `journal_entries` unique `(company_id, reference_number)`  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalEntry.java`
- Canonical/legacy resolution in `JournalReferenceResolver`  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/JournalReferenceResolver.java`
- Mapping table + legacy remap rules  
  `erp-domain/src/main/resources/db/migration/V88__journal_reference_mappings.sql`

## Invoices
- Unique invoice number per company  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/Invoice.java`
- Payment idempotency via `invoice_payment_refs` (reference set)  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/Invoice.java`
- Settlement policy uses reference to avoid duplicate payment application  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceSettlementPolicy.java`

## Settlements / Receipts
- Idempotency key column on `partner_settlement_allocations`  
  `erp-domain/src/main/resources/db/migration/V48__settlement_idempotency_keys.sql`
- Current scope: non‑unique indexes on `(company_id, idempotency_key[, invoice_id|purchase_id])`  
  `erp-domain/src/main/resources/db/migration/V102__partner_settlement_idempotency_scope.sql`
- Usage: `AccountingService.recordDealerReceipt(...)` and `settleDealerInvoices(...)` check for existing allocations  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java`

## Inventory Movements
- Reference fields: `reference_type`, `reference_id`; indexed (non‑unique)  
  `erp-domain/src/main/resources/db/migration/V9__finished_goods_inventory.sql`  
  `erp-domain/src/main/resources/db/migration/V97__performance_list_indexes.sql`
- Entity has no unique constraint on reference tuple  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryMovement.java`

## Noted Gaps / Follow‑ups
- `journal_reference_mappings` has unique on legacy reference only; canonical reference is indexed but not unique  
  `erp-domain/src/main/resources/db/migration/V88__journal_reference_mappings.sql`
- `inventory_movements` relies on code‑level idempotency; no DB uniqueness on reference tuple  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryMovement.java`
