-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Detect duplicate idempotency keys in supplier settlement allocations.

SELECT
  'settlement_duplicate_idempotency_key' AS finding_type,
  psa.idempotency_key,
  COUNT(*) AS allocation_rows,
  MIN(psa.id) AS min_id,
  MAX(psa.id) AS max_id
FROM partner_settlement_allocations psa
WHERE psa.company_id = :company_id
  AND psa.idempotency_key IS NOT NULL
  AND psa.idempotency_key <> ''
GROUP BY psa.idempotency_key
HAVING COUNT(*) > 1
ORDER BY allocation_rows DESC, max_id DESC;
