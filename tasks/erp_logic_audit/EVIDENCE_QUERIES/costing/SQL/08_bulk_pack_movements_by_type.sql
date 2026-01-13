-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Summarize movements for a specific bulk-pack reference by movement type,
--   including journal linkage (issue vs receipt).
--
-- Optional params:
--   :pack_reference (text)

\if :{?pack_reference}
\else
\set pack_reference ''
\endif

SELECT
  im.reference_id AS pack_reference,
  im.movement_type,
  COUNT(*) AS movement_count,
  COUNT(*) FILTER (WHERE im.journal_entry_id IS NULL) AS movements_missing_je_link,
  MIN(im.journal_entry_id) AS min_journal_entry_id,
  MAX(im.journal_entry_id) AS max_journal_entry_id
FROM inventory_movements im
JOIN finished_goods fg ON fg.id = im.finished_good_id
WHERE fg.company_id = :company_id
  AND im.reference_type = 'PACKAGING'
  AND im.reference_id LIKE 'PACK-%'
  AND (:'pack_reference' = '' OR im.reference_id = :'pack_reference')
GROUP BY im.reference_id, im.movement_type
ORDER BY im.reference_id, im.movement_type;
