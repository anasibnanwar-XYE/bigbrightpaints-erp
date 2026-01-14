--   :company_id (numeric)
--   :reference (string)
--
-- Purpose:
--   Check purchase return journals + raw material movements for a reference number.

SELECT
  id,
  reference_number,
  entry_date,
  supplier_id,
  created_at
FROM journal_entries
WHERE company_id = :company_id
  AND reference_number = :'reference'
ORDER BY id;

SELECT
  m.reference_id,
  m.movement_type,
  COUNT(*) AS movement_count,
  SUM(m.quantity) AS total_quantity,
  MIN(m.created_at) AS first_seen,
  MAX(m.created_at) AS last_seen
FROM raw_material_movements m
JOIN raw_materials rm ON rm.id = m.raw_material_id
WHERE rm.company_id = :company_id
  AND m.reference_type = 'PURCHASE_RETURN'
  AND m.reference_id = :'reference'
GROUP BY m.reference_id, m.movement_type
ORDER BY movement_count DESC, last_seen DESC;
