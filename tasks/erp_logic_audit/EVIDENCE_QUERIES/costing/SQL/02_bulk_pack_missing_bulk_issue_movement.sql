-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Show bulk-to-size parent batches that have child RECEIPT movements,
--   but no corresponding ISSUE movement for the parent bulk batch under the same pack reference.
--
-- Notes:
--   Pack reference is deterministic and shared across the related movements:
--     'PACK-' || parent_batch.batch_code || '-' || <stable_hash>
--
-- Optional params:
--   :pack_reference (text)

\if :{?pack_reference}
\else
\set pack_reference ''
\endif

WITH child_receipts AS (
  SELECT
    im.reference_id,
    pb.id AS parent_batch_id,
    pb.batch_code AS parent_batch_code,
    fg.product_code AS bulk_product_code,
    COUNT(*) AS receipt_movements_for_pack_ref,
    COALESCE(SUM(im.quantity), 0) AS receipt_qty_for_pack_ref
  FROM inventory_movements im
  JOIN finished_good_batches cb ON cb.id = im.finished_good_batch_id
  JOIN finished_good_batches pb ON pb.id = cb.parent_batch_id
  JOIN finished_goods fg ON fg.id = pb.finished_good_id
  WHERE fg.company_id = :company_id
    AND im.reference_type = 'PACKAGING'
    AND im.movement_type = 'RECEIPT'
    AND (:'pack_reference' = '' OR im.reference_id = :'pack_reference')
  GROUP BY im.reference_id, pb.id, pb.batch_code, fg.product_code
),
issue_movements AS (
  SELECT
    im.reference_id,
    pb.id AS parent_batch_id,
    COUNT(*) AS issue_movements_for_pack_ref,
    COALESCE(SUM(im.quantity), 0) AS issue_qty_for_pack_ref
  FROM inventory_movements im
  JOIN finished_good_batches pb ON pb.id = im.finished_good_batch_id
  JOIN finished_goods fg ON fg.id = pb.finished_good_id
  WHERE fg.company_id = :company_id
    AND im.reference_type = 'PACKAGING'
    AND im.movement_type = 'ISSUE'
    AND (:'pack_reference' = '' OR im.reference_id = :'pack_reference')
  GROUP BY im.reference_id, pb.id
)
SELECT
  cr.parent_batch_id,
  cr.parent_batch_code,
  cr.bulk_product_code,
  cr.reference_id AS pack_reference,
  cr.receipt_movements_for_pack_ref,
  cr.receipt_qty_for_pack_ref,
  COALESCE(im.issue_movements_for_pack_ref, 0) AS issue_movements_for_pack_ref,
  COALESCE(im.issue_qty_for_pack_ref, 0) AS issue_qty_for_pack_ref
FROM child_receipts cr
LEFT JOIN issue_movements im
  ON im.reference_id = cr.reference_id
  AND im.parent_batch_id = cr.parent_batch_id
WHERE COALESCE(im.issue_movements_for_pack_ref, 0) = 0
ORDER BY cr.parent_batch_id DESC;
