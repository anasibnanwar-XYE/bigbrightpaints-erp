-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Identify production products and missing metadata needed for WIP postings.

SELECT
  pp.id,
  pp.brand_id,
  pb.name AS brand_name,
  pp.sku_code,
  pp.product_name,
  pp.category,
  pp.is_active,
  pp.metadata->>'wipAccountId' AS wip_account_id,
  pp.metadata->>'semiFinishedAccountId' AS semi_finished_account_id,
  pp.metadata->>'fgValuationAccountId' AS fg_valuation_account_id
FROM production_products pp
JOIN production_brands pb ON pb.id = pp.brand_id
WHERE pp.company_id = :company_id
ORDER BY pp.id;
