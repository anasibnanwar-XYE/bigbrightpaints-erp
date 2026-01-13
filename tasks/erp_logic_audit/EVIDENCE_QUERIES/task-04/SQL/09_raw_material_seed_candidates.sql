-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   List raw materials and batches with available quantities for production log seeding.

SELECT
  rm.id AS raw_material_id,
  rm.sku,
  rm.name AS raw_material_name,
  rm.unit_type,
  rm.current_stock,
  rm.inventory_account_id,
  rmb.id AS batch_id,
  rmb.batch_code,
  rmb.quantity AS batch_quantity,
  rmb.cost_per_unit,
  rmb.received_at
FROM raw_materials rm
LEFT JOIN raw_material_batches rmb ON rmb.raw_material_id = rm.id
WHERE rm.company_id = :company_id
  AND (rmb.quantity IS NULL OR rmb.quantity > 0)
ORDER BY rm.current_stock DESC, rmb.received_at DESC NULLS LAST;
