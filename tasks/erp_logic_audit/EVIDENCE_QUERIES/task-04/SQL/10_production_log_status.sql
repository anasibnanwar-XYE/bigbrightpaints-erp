-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Inspect recent production logs for packed quantity, status, and cost components.

SELECT
  pl.id,
  pl.production_code,
  pl.status,
  pl.mixed_quantity,
  pl.total_packed_quantity,
  pl.wastage_quantity,
  pl.material_cost_total,
  pl.labor_cost_total,
  pl.overhead_cost_total,
  pl.unit_cost
FROM production_logs pl
WHERE pl.company_id = :company_id
ORDER BY pl.id DESC
LIMIT 5;
