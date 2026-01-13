-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Inspect recent production logs for packed quantity and status.

SELECT
  pl.id,
  pl.production_code,
  pl.status,
  pl.mixed_quantity,
  pl.total_packed_quantity,
  pl.wastage_quantity
FROM production_logs pl
WHERE pl.company_id = :company_id
ORDER BY pl.id DESC
LIMIT 5;
