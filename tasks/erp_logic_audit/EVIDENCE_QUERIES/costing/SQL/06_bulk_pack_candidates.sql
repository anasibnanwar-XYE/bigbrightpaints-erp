-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Help pick safe dev-only inputs for bulk-to-size packing probes.

-- Candidate bulk batches (is_bulk=true) with available qty
SELECT
  pb.id AS bulk_batch_id,
  pb.batch_code,
  pb.quantity_available,
  pb.unit_cost,
  fg.id AS bulk_finished_good_id,
  fg.product_code AS bulk_product_code
FROM finished_good_batches pb
JOIN finished_goods fg ON fg.id = pb.finished_good_id
WHERE fg.company_id = :company_id
  AND COALESCE(pb.is_bulk, false) = true
  AND pb.quantity_available > 0
ORDER BY pb.id DESC
LIMIT 25;

-- Candidate child SKUs (non-bulk product_code)
SELECT
  fg.id AS child_sku_id,
  fg.product_code,
  fg.name,
  fg.current_stock
FROM finished_goods fg
WHERE fg.company_id = :company_id
  AND fg.product_code NOT LIKE '%-BULK'
ORDER BY fg.id DESC
LIMIT 25;
