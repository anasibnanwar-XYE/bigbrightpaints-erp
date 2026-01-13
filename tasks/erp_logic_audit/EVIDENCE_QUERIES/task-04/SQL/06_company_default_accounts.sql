-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Capture company default account IDs used for production posting defaults.

SELECT
  id AS company_id,
  code AS company_code,
  default_inventory_account_id,
  default_cogs_account_id,
  default_revenue_account_id,
  default_discount_account_id,
  default_tax_account_id
FROM companies
WHERE id = :company_id;
