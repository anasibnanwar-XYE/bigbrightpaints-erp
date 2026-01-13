-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   List candidate WIP/Inventory accounts for production metadata seeding.

SELECT
  id,
  code,
  name,
  type
FROM accounts
WHERE company_id = :company_id
  AND (
    code ILIKE '%WIP%'
    OR name ILIKE '%WIP%'
    OR code ILIKE '%INV%'
    OR name ILIKE '%INVENT%'
  )
ORDER BY code NULLS LAST, id;
