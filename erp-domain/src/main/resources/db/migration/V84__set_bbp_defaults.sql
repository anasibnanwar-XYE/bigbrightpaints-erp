-- Set company default accounts for BBP based on common chart codes.
-- Inventory: 1200, COGS: 5000, Revenue: 4000, Tax: 2000 (liability), Discount left null if not found.
UPDATE companies c SET
    default_inventory_account_id = (SELECT a.id FROM accounts a WHERE a.company_id = c.id AND UPPER(a.code) = '1200' LIMIT 1),
    default_cogs_account_id = (SELECT a.id FROM accounts a WHERE a.company_id = c.id AND UPPER(a.code) = '5000' LIMIT 1),
    default_revenue_account_id = (SELECT a.id FROM accounts a WHERE a.company_id = c.id AND UPPER(a.code) = '4000' LIMIT 1),
    default_tax_account_id = (SELECT a.id FROM accounts a WHERE a.company_id = c.id AND UPPER(a.code) = '2000' LIMIT 1)
WHERE UPPER(c.code) = 'BBP';
