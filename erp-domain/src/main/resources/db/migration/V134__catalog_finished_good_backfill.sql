-- Ensure every non-raw production SKU has a matching finished_good row.
-- This hardens sales/dispatch flows that resolve inventory by product_code.

WITH resolved_products AS (
    SELECT p.company_id,
           p.sku_code,
           p.product_name,
           COALESCE(NULLIF(TRIM(p.unit_of_measure), ''), 'UNIT') AS unit_of_measure,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgValuationAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgValuationAccountId')::BIGINT
               END,
               c.default_inventory_account_id
           ) AS valuation_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgCogsAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgCogsAccountId')::BIGINT
               END,
               c.default_cogs_account_id
           ) AS cogs_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgRevenueAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgRevenueAccountId')::BIGINT
               END,
               c.default_revenue_account_id
           ) AS revenue_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgDiscountAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgDiscountAccountId')::BIGINT
               END,
               c.default_discount_account_id
           ) AS discount_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgTaxAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgTaxAccountId')::BIGINT
               END,
               c.default_tax_account_id
           ) AS tax_account_id
    FROM production_products p
             JOIN companies c ON c.id = p.company_id
    WHERE UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
      AND p.sku_code IS NOT NULL
      AND TRIM(p.sku_code) <> ''
)
INSERT INTO finished_goods (company_id,
                            product_code,
                            name,
                            unit,
                            current_stock,
                            reserved_stock,
                            costing_method,
                            valuation_account_id,
                            cogs_account_id,
                            revenue_account_id,
                            discount_account_id,
                            tax_account_id)
SELECT rp.company_id,
       rp.sku_code,
       rp.product_name,
       rp.unit_of_measure,
       0,
       0,
       'FIFO',
       rp.valuation_account_id,
       rp.cogs_account_id,
       rp.revenue_account_id,
       rp.discount_account_id,
       rp.tax_account_id
FROM resolved_products rp
ON CONFLICT (company_id, product_code) DO NOTHING;

WITH resolved_products AS (
    SELECT p.company_id,
           p.sku_code,
           p.product_name,
           COALESCE(NULLIF(TRIM(p.unit_of_measure), ''), 'UNIT') AS unit_of_measure,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgValuationAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgValuationAccountId')::BIGINT
               END,
               c.default_inventory_account_id
           ) AS valuation_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgCogsAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgCogsAccountId')::BIGINT
               END,
               c.default_cogs_account_id
           ) AS cogs_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgRevenueAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgRevenueAccountId')::BIGINT
               END,
               c.default_revenue_account_id
           ) AS revenue_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgDiscountAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgDiscountAccountId')::BIGINT
               END,
               c.default_discount_account_id
           ) AS discount_account_id,
           COALESCE(
               CASE
                   WHEN COALESCE(p.metadata ->> 'fgTaxAccountId', '') ~ '^[0-9]+$'
                       THEN (p.metadata ->> 'fgTaxAccountId')::BIGINT
               END,
               c.default_tax_account_id
           ) AS tax_account_id
    FROM production_products p
             JOIN companies c ON c.id = p.company_id
    WHERE UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
      AND p.sku_code IS NOT NULL
      AND TRIM(p.sku_code) <> ''
)
UPDATE finished_goods fg
SET name = rp.product_name,
    unit = rp.unit_of_measure,
    valuation_account_id = COALESCE(fg.valuation_account_id, rp.valuation_account_id),
    cogs_account_id = COALESCE(fg.cogs_account_id, rp.cogs_account_id),
    revenue_account_id = COALESCE(fg.revenue_account_id, rp.revenue_account_id),
    discount_account_id = COALESCE(fg.discount_account_id, rp.discount_account_id),
    tax_account_id = COALESCE(fg.tax_account_id, rp.tax_account_id)
FROM resolved_products rp
WHERE fg.company_id = rp.company_id
  AND fg.product_code = rp.sku_code;
