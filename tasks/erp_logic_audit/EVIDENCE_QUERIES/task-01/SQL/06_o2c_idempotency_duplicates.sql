-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Detect duplicate idempotency keys in sales orders.

SELECT
  'sales_order_duplicate_idempotency_key' AS finding_type,
  so.idempotency_key,
  COUNT(*) AS order_count,
  MIN(so.id) AS min_order_id,
  MAX(so.id) AS max_order_id
FROM sales_orders so
WHERE so.company_id = :company_id
  AND so.idempotency_key IS NOT NULL
  AND so.idempotency_key <> ''
GROUP BY so.idempotency_key
HAVING COUNT(*) > 1
ORDER BY order_count DESC, max_order_id DESC;
