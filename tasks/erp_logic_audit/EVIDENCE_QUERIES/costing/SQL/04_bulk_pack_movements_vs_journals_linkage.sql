-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Compare bulk-pack inventory movements (pack reference_id) against journals
--   using the same deterministic reference number, highlighting missing linkage.
--
-- Optional params:
--   :pack_reference (text)

\if :{?pack_reference}
\else
\set pack_reference ''
\endif

WITH movements AS (
  SELECT
    im.reference_id,
    COUNT(*) AS movement_count,
    COUNT(*) FILTER (WHERE im.journal_entry_id IS NULL) AS movements_missing_je_link
  FROM inventory_movements im
  JOIN finished_goods fg ON fg.id = im.finished_good_id
  WHERE fg.company_id = :company_id
    AND im.reference_type = 'PACKAGING'
    AND im.reference_id LIKE 'PACK-%'
    AND (:'pack_reference' = '' OR im.reference_id = :'pack_reference')
  GROUP BY im.reference_id
),
pack_journals AS (
  SELECT
    reference_number,
    COUNT(*) AS journal_count
  FROM journal_entries
  WHERE company_id = :company_id
    AND reference_number LIKE 'PACK-%'
    AND (:'pack_reference' = '' OR reference_number = :'pack_reference')
  GROUP BY reference_number
)
SELECT
  m.reference_id AS pack_reference,
  m.movement_count,
  m.movements_missing_je_link,
  COALESCE(j.journal_count, 0) AS journal_count
FROM movements m
LEFT JOIN pack_journals j ON j.reference_number = m.reference_id
ORDER BY m.movements_missing_je_link DESC, m.reference_id;
