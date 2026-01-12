-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Raw material movements for purchases/returns missing journal links.

SELECT
  'rm_movement_missing_je' AS finding_type,
  rmm.id AS movement_id,
  rm.sku,
  rmm.reference_type,
  rmm.reference_id,
  rmm.movement_type,
  rmm.quantity,
  rmm.unit_cost,
  rmm.journal_entry_id,
  rmm.created_at
FROM raw_material_movements rmm
JOIN raw_materials rm ON rm.id = rmm.raw_material_id
WHERE rm.company_id = :company_id
  AND rmm.reference_type IN ('RAW_MATERIAL_PURCHASE', 'PURCHASE_RETURN')
  AND rmm.journal_entry_id IS NULL
ORDER BY rmm.created_at DESC, rmm.id DESC;
