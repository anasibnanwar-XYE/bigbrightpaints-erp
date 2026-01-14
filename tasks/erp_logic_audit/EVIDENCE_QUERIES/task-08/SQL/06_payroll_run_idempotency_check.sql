--   :company_id (numeric)
--   :idempotency_key (string)
--
-- Purpose:
--   Confirm payroll run idempotency behavior for a specific key.

SELECT
  id,
  run_date,
  status,
  total_amount,
  idempotency_key,
  created_at
FROM payroll_runs
WHERE company_id = :company_id
  AND idempotency_key = :'idempotency_key'
ORDER BY id;

SELECT
  idempotency_key,
  COUNT(*) AS run_count
FROM payroll_runs
WHERE company_id = :company_id
  AND idempotency_key = :'idempotency_key'
GROUP BY idempotency_key;
