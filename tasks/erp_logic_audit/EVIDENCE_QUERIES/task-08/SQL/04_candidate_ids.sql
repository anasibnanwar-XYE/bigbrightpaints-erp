--   :company_id (numeric)
--
-- Purpose:
--   Capture candidate IDs for idempotency probes (sales, purchasing, factory).

-- Dealers (sales orders)
SELECT
  id,
  code,
  name
FROM dealers
WHERE company_id = :company_id
ORDER BY id
LIMIT 10;

-- Suppliers (purchase returns)
SELECT
  id,
  code,
  name
FROM suppliers
WHERE company_id = :company_id
ORDER BY id
LIMIT 10;

-- Raw materials (purchase returns)
SELECT
  id,
  sku,
  name
FROM raw_materials
WHERE company_id = :company_id
ORDER BY id
LIMIT 10;

-- Finished goods (sales orders / bulk pack child SKU)
SELECT
  id,
  product_code,
  name
FROM finished_goods
WHERE company_id = :company_id
ORDER BY id
LIMIT 10;

-- Finished good batches (bulk pack source)
SELECT
  fgb.id AS batch_id,
  fgb.batch_code,
  fgb.quantity_available,
  fgb.is_bulk,
  fgb.parent_batch_id,
  fgb.finished_good_id,
  fg.product_code
FROM finished_good_batches fgb
JOIN finished_goods fg ON fg.id = fgb.finished_good_id
WHERE fg.company_id = :company_id
ORDER BY fgb.id DESC
LIMIT 10;
