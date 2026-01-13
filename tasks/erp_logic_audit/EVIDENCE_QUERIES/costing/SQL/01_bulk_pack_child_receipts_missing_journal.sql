-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Identify BulkPackingService child FG receipt movements that have no journal_entry_id linkage.
--
-- Notes:
--   BulkPackingService records inventory movements with:
--     reference_type = 'PACKAGING'
--     reference_id   = 'PACK-' || <parent_batch_code> || '-' || <stable_hash>
--     movement_type  = 'RECEIPT'
--
-- Optional params:
--   :pack_reference (text)

\if :{?pack_reference}
\else
\set pack_reference ''
\endif

SELECT
  im.id AS movement_id,
  fg.product_code AS child_product_code,
  im.reference_type,
  im.reference_id,
  im.movement_type,
  im.quantity,
  im.unit_cost,
  im.journal_entry_id,
  im.created_at,
  cb.id AS child_batch_id,
  cb.batch_code AS child_batch_code,
  pb.id AS parent_batch_id,
  pb.batch_code AS parent_batch_code
FROM inventory_movements im
JOIN finished_goods fg ON fg.id = im.finished_good_id
JOIN finished_good_batches cb ON cb.id = im.finished_good_batch_id
LEFT JOIN finished_good_batches pb ON pb.id = cb.parent_batch_id
WHERE fg.company_id = :company_id
  AND cb.parent_batch_id IS NOT NULL
  AND im.reference_type = 'PACKAGING'
  AND im.reference_id LIKE 'PACK-%'
  AND (:'pack_reference' = '' OR im.reference_id = :'pack_reference')
  AND im.movement_type = 'RECEIPT'
  AND im.journal_entry_id IS NULL
ORDER BY im.created_at DESC, im.id DESC;
