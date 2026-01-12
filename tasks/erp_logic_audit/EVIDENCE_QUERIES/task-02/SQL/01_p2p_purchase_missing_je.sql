-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Detect posted purchases missing journal links.

SELECT
  'purchase_missing_je' AS finding_type,
  p.id AS purchase_id,
  p.invoice_number,
  p.status,
  p.invoice_date,
  p.journal_entry_id
FROM raw_material_purchases p
WHERE p.company_id = :company_id
  AND p.status = 'POSTED'
  AND p.journal_entry_id IS NULL
ORDER BY p.invoice_date DESC, p.id DESC;
