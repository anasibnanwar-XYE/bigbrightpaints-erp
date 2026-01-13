-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Detect duplicate bulk-pack journals that should be idempotent.
--
-- Notes:
--   BulkPackingService now uses deterministic references:
--     'PACK-' || bulk_batch.batch_code || '-' || <stable_hash>
--
-- Optional params:
--   :pack_reference (text)

\if :{?pack_reference}
\else
\set pack_reference ''
\endif

SELECT
  reference_number,
  COUNT(*) AS journal_count,
  MIN(created_at) AS first_created_at,
  MAX(created_at) AS last_created_at
FROM journal_entries
WHERE company_id = :company_id
  AND reference_number LIKE 'PACK-%'
  AND (:'pack_reference' = '' OR reference_number = :'pack_reference')
GROUP BY reference_number
ORDER BY journal_count DESC, reference_number;
