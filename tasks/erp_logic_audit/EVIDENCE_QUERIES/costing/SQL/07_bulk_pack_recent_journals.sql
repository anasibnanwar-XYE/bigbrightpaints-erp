-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Show recent bulk-pack journal entries and deterministic reference numbers.
--
-- Optional params:
--   :pack_reference (text)

\if :{?pack_reference}
\else
\set pack_reference ''
\endif

SELECT
  je.id AS journal_entry_id,
  je.reference_number,
  je.entry_date,
  je.status,
  je.created_at
FROM journal_entries je
WHERE je.company_id = :company_id
  AND je.reference_number LIKE 'PACK-%'
  AND (:'pack_reference' = '' OR je.reference_number = :'pack_reference')
ORDER BY je.created_at DESC, je.id DESC
LIMIT 50;
