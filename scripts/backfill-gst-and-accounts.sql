\set ON_ERROR_STOP on

BEGIN;

-- 1. Hydrate finished good revenue/discount/tax accounts from product metadata
WITH metadata_accounts AS (
    SELECT fg.id,
           NULLIF(pp.metadata ->> 'fgRevenueAccountId', '')::BIGINT   AS revenue_account_id,
           NULLIF(pp.metadata ->> 'fgDiscountAccountId', '')::BIGINT  AS discount_account_id,
           NULLIF(pp.metadata ->> 'fgTaxAccountId', '')::BIGINT       AS tax_account_id
    FROM finished_goods fg
    LEFT JOIN production_products pp
        ON pp.company_id = fg.company_id
       AND pp.sku_code = fg.product_code
),
revenue_defaults AS (
    SELECT company_id, MIN(id) AS revenue_account_id
    FROM accounts
    WHERE UPPER(type) = 'REVENUE'
    GROUP BY company_id
),
tax_defaults AS (
    SELECT company_id,
           MIN(id) FILTER (WHERE LOWER(name) LIKE '%gst%') AS gst_specific,
           MIN(id) FILTER (WHERE LOWER(name) NOT LIKE '%gst%') AS fallback_liability
    FROM accounts
    WHERE UPPER(type) = 'LIABILITY'
    GROUP BY company_id
),
discount_defaults AS (
    SELECT company_id, MIN(id) AS discount_account_id
    FROM accounts
    WHERE UPPER(type) = 'CONTRA_REVENUE'
       OR UPPER(name) LIKE '%DISCOUNT%'
    GROUP BY company_id
)
UPDATE finished_goods fg
SET revenue_account_id = COALESCE(
        fg.revenue_account_id,
        (SELECT m.revenue_account_id FROM metadata_accounts m WHERE m.id = fg.id),
        (SELECT r.revenue_account_id FROM revenue_defaults r WHERE r.company_id = fg.company_id)
    ),
    discount_account_id = COALESCE(
        fg.discount_account_id,
        (SELECT m.discount_account_id FROM metadata_accounts m WHERE m.id = fg.id),
        (SELECT d.discount_account_id FROM discount_defaults d WHERE d.company_id = fg.company_id),
        (SELECT r.revenue_account_id FROM revenue_defaults r WHERE r.company_id = fg.company_id)
    ),
    tax_account_id = COALESCE(
        fg.tax_account_id,
        (SELECT m.tax_account_id FROM metadata_accounts m WHERE m.id = fg.id),
        (SELECT COALESCE(t.gst_specific, t.fallback_liability) FROM tax_defaults t WHERE t.company_id = fg.company_id)
    );

-- 2. Normalize existing sales order items with line subtotals/totals
WITH line_totals AS (
    SELECT id,
           ROUND(quantity * unit_price, 2) AS line_subtotal
    FROM sales_order_items
)
UPDATE sales_order_items soi
SET line_subtotal = lt.line_subtotal,
    line_total = lt.line_subtotal,
    gst_rate = COALESCE(soi.gst_rate, 0),
    gst_amount = 0
FROM line_totals lt
WHERE soi.id = lt.id;

-- 3. Roll the new line values up to their orders
WITH order_totals AS (
    SELECT sales_order_id,
           ROUND(SUM(line_subtotal), 2) AS subtotal
    FROM sales_order_items
    GROUP BY sales_order_id
)
UPDATE sales_orders so
SET subtotal_amount = COALESCE(ot.subtotal, so.total_amount),
    gst_total = 0,
    gst_treatment = 'NONE',
    gst_rate = 0,
    gst_rounding_adjustment = 0
FROM order_totals ot
WHERE so.id = ot.sales_order_id;

-- 4. Guard orders with no items
UPDATE sales_orders
SET subtotal_amount = total_amount,
    gst_total = 0,
    gst_treatment = 'NONE',
    gst_rate = 0,
    gst_rounding_adjustment = 0
WHERE subtotal_amount IS NULL OR subtotal_amount = 0;

COMMIT;
