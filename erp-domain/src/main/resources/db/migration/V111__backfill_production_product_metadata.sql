-- Backfill required account metadata for production products to satisfy configuration health checks.
-- Uses company defaults with sensible account-code fallbacks.

-- Ensure a WIP account exists for production metadata defaults.
INSERT INTO accounts (company_id, code, name, type, balance)
SELECT c.id, 'WIP', 'Work in Progress', 'ASSET', 0
FROM companies c
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a
    WHERE a.company_id = c.id AND UPPER(a.code) IN ('WIP', '1170', '1180')
);

-- Ensure base currency is set (required by configuration health).
UPDATE companies
SET base_currency = 'INR'
WHERE base_currency IS NULL OR TRIM(base_currency) = '';

DROP TABLE IF EXISTS tmp_company_accounts;
CREATE TEMP TABLE tmp_company_accounts AS
WITH base_accounts AS (
    SELECT c.id AS company_id,
           COALESCE(
               c.default_inventory_account_id,
               (SELECT a.id
                FROM accounts a
                WHERE a.company_id = c.id AND UPPER(a.code) IN ('INV', '1200')
                ORDER BY CASE UPPER(a.code) WHEN 'INV' THEN 1 ELSE 2 END, a.id
                LIMIT 1)
           ) AS inv_id,
           COALESCE(
               c.default_cogs_account_id,
               (SELECT a.id
                FROM accounts a
                WHERE a.company_id = c.id AND UPPER(a.code) IN ('COGS', '5000')
                ORDER BY CASE UPPER(a.code) WHEN 'COGS' THEN 1 ELSE 2 END, a.id
                LIMIT 1)
           ) AS cogs_id,
           COALESCE(
               c.default_revenue_account_id,
               (SELECT a.id
                FROM accounts a
                WHERE a.company_id = c.id AND UPPER(a.code) IN ('REV', '4000')
                ORDER BY CASE UPPER(a.code) WHEN 'REV' THEN 1 ELSE 2 END, a.id
                LIMIT 1)
           ) AS rev_id,
           COALESCE(
               c.default_discount_account_id,
               (SELECT a.id
                FROM accounts a
                WHERE a.company_id = c.id AND UPPER(a.code) = 'DISC'
                ORDER BY a.id
                LIMIT 1)
           ) AS disc_id,
           COALESCE(
               c.default_tax_account_id,
               (SELECT a.id
                FROM accounts a
                WHERE a.company_id = c.id AND UPPER(a.code) IN ('GST-OUT', '2100')
                ORDER BY CASE UPPER(a.code) WHEN 'GST-OUT' THEN 1 ELSE 2 END, a.id
                LIMIT 1)
           ) AS tax_id
    FROM companies c
)
SELECT b.company_id,
       b.inv_id,
       b.cogs_id,
       b.rev_id,
       b.disc_id,
       b.tax_id,
       COALESCE(
           (SELECT a.id
            FROM accounts a
            WHERE a.company_id = b.company_id AND UPPER(a.code) IN ('WIP', '1170', '1180')
            ORDER BY CASE UPPER(a.code)
                     WHEN 'WIP' THEN 1
                     WHEN '1170' THEN 2
                     ELSE 3
                     END,
                     a.id
            LIMIT 1),
           b.inv_id
       ) AS wip_id
FROM base_accounts b;

-- Backfill raw material inventory accounts.
UPDATE raw_materials rm
SET inventory_account_id = ca.inv_id
FROM tmp_company_accounts ca
WHERE rm.company_id = ca.company_id
  AND rm.inventory_account_id IS NULL
  AND ca.inv_id IS NOT NULL;

-- Backfill finished goods account references.
UPDATE finished_goods fg
SET valuation_account_id = COALESCE(fg.valuation_account_id, ca.inv_id),
    cogs_account_id = COALESCE(fg.cogs_account_id, ca.cogs_id),
    revenue_account_id = COALESCE(fg.revenue_account_id, ca.rev_id),
    discount_account_id = COALESCE(fg.discount_account_id, ca.disc_id),
    tax_account_id = COALESCE(fg.tax_account_id, ca.tax_id)
FROM tmp_company_accounts ca
WHERE fg.company_id = ca.company_id
  AND (
      fg.valuation_account_id IS NULL OR
      fg.cogs_account_id IS NULL OR
      fg.revenue_account_id IS NULL OR
      fg.discount_account_id IS NULL OR
      fg.tax_account_id IS NULL
  );

-- Production product metadata backfill (non-raw-material categories).
UPDATE production_products p
SET metadata = jsonb_set(COALESCE(p.metadata, '{}'::jsonb), '{fgValuationAccountId}', to_jsonb(ca.inv_id), true)
FROM tmp_company_accounts ca
WHERE p.company_id = ca.company_id
  AND UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
  AND (p.metadata ->> 'fgValuationAccountId') IS NULL
  AND ca.inv_id IS NOT NULL;

UPDATE production_products p
SET metadata = jsonb_set(COALESCE(p.metadata, '{}'::jsonb), '{fgCogsAccountId}', to_jsonb(ca.cogs_id), true)
FROM tmp_company_accounts ca
WHERE p.company_id = ca.company_id
  AND UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
  AND (p.metadata ->> 'fgCogsAccountId') IS NULL
  AND ca.cogs_id IS NOT NULL;

UPDATE production_products p
SET metadata = jsonb_set(COALESCE(p.metadata, '{}'::jsonb), '{fgRevenueAccountId}', to_jsonb(ca.rev_id), true)
FROM tmp_company_accounts ca
WHERE p.company_id = ca.company_id
  AND UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
  AND (p.metadata ->> 'fgRevenueAccountId') IS NULL
  AND ca.rev_id IS NOT NULL;

UPDATE production_products p
SET metadata = jsonb_set(COALESCE(p.metadata, '{}'::jsonb), '{fgDiscountAccountId}', to_jsonb(ca.disc_id), true)
FROM tmp_company_accounts ca
WHERE p.company_id = ca.company_id
  AND UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
  AND (p.metadata ->> 'fgDiscountAccountId') IS NULL
  AND ca.disc_id IS NOT NULL;

UPDATE production_products p
SET metadata = jsonb_set(COALESCE(p.metadata, '{}'::jsonb), '{fgTaxAccountId}', to_jsonb(ca.tax_id), true)
FROM tmp_company_accounts ca
WHERE p.company_id = ca.company_id
  AND UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
  AND (p.metadata ->> 'fgTaxAccountId') IS NULL
  AND ca.tax_id IS NOT NULL;

UPDATE production_products p
SET metadata = jsonb_set(COALESCE(p.metadata, '{}'::jsonb), '{wipAccountId}', to_jsonb(ca.wip_id), true)
FROM tmp_company_accounts ca
WHERE p.company_id = ca.company_id
  AND UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
  AND (p.metadata ->> 'wipAccountId') IS NULL
  AND ca.wip_id IS NOT NULL;

UPDATE production_products p
SET metadata = jsonb_set(COALESCE(p.metadata, '{}'::jsonb), '{semiFinishedAccountId}', to_jsonb(ca.inv_id), true)
FROM tmp_company_accounts ca
WHERE p.company_id = ca.company_id
  AND UPPER(COALESCE(p.category, '')) NOT IN ('RAW_MATERIAL', 'RAW MATERIAL', 'RAW-MATERIAL')
  AND (p.metadata ->> 'semiFinishedAccountId') IS NULL
  AND ca.inv_id IS NOT NULL;

DROP TABLE IF EXISTS tmp_company_accounts;
