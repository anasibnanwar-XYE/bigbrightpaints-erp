--   :company_id (numeric)
--   :idempotency_key (string)
--
-- Purpose:
--   Confirm sales order idempotency behavior for a specific key.

SELECT
  id,
  order_number,
  status,
  total_amount,
  idempotency_key,
  created_at
FROM sales_orders
WHERE company_id = :company_id
  AND idempotency_key = :'idempotency_key'
ORDER BY id;

SELECT
  idempotency_key,
  COUNT(*) AS order_count
FROM sales_orders
WHERE company_id = :company_id
  AND idempotency_key = :'idempotency_key'
GROUP BY idempotency_key;
