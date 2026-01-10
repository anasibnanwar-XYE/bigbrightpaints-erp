-- Seed a minimal OPS company, admin user, and default accounts for local API checks.
-- Password hash corresponds to OpsPass123! (bcrypt).

WITH company_row AS (
    INSERT INTO companies (name, code, timezone)
    VALUES ('Ops Company', 'OPS', 'UTC')
    ON CONFLICT (code) DO NOTHING
    RETURNING id
),
company_id AS (
    SELECT id FROM company_row
    UNION ALL
    SELECT id FROM companies WHERE code = 'OPS'
),
admin_role AS (
    INSERT INTO roles (name, description)
    VALUES ('ROLE_ADMIN', 'Platform administrator')
    ON CONFLICT (name) DO NOTHING
    RETURNING id
),
admin_role_id AS (
    SELECT id FROM admin_role
    UNION ALL
    SELECT id FROM roles WHERE name = 'ROLE_ADMIN'
),
admin_perms AS (
    SELECT id FROM permissions WHERE code IN ('dispatch.confirm', 'factory.dispatch', 'payroll.run')
),
insert_admin_perms AS (
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT (SELECT id FROM admin_role_id), id FROM admin_perms
    ON CONFLICT DO NOTHING
),
user_row AS (
    INSERT INTO app_users (email, password_hash, display_name, enabled)
    VALUES ('ops.admin@example.com', '$2b$12$TsFA7A2raV7QxQSqY.WsS.oSCi9h45/1ZsfD5fwKG2B7l/RU5aYdC', 'Ops Admin', true)
    ON CONFLICT (email) DO NOTHING
    RETURNING id
),
user_id AS (
    SELECT id FROM user_row
    UNION ALL
    SELECT id FROM app_users WHERE email = 'ops.admin@example.com'
),
role_ids AS (
    SELECT id FROM roles WHERE name IN ('ROLE_ADMIN', 'ROLE_ACCOUNTING', 'ROLE_FACTORY', 'ROLE_SALES')
),
insert_user_roles AS (
    INSERT INTO user_roles (user_id, role_id)
    SELECT (SELECT id FROM user_id), id FROM role_ids
    ON CONFLICT DO NOTHING
),
insert_user_company AS (
    INSERT INTO user_companies (user_id, company_id)
    SELECT (SELECT id FROM user_id), (SELECT id FROM company_id)
    ON CONFLICT DO NOTHING
)
SELECT (SELECT id FROM user_id) AS user_id,
       (SELECT id FROM company_id) AS company_id;

INSERT INTO accounts (id, company_id, code, name, type, balance)
SELECT * FROM (VALUES
    (1000, (SELECT id FROM companies WHERE code = 'OPS'), '1000', 'Cash', 'ASSET', 0.00),
    (1100, (SELECT id FROM companies WHERE code = 'OPS'), '1100', 'Accounts Receivable', 'ASSET', 0.00),
    (1200, (SELECT id FROM companies WHERE code = 'OPS'), '1200', 'Inventory', 'ASSET', 0.00),
    (2000, (SELECT id FROM companies WHERE code = 'OPS'), '2000', 'Accounts Payable', 'LIABILITY', 0.00),
    (2100, (SELECT id FROM companies WHERE code = 'OPS'), '2100', 'GST Payable', 'LIABILITY', 0.00),
    (2200, (SELECT id FROM companies WHERE code = 'OPS'), '2200', 'GST Input', 'ASSET', 0.00),
    (2300, (SELECT id FROM companies WHERE code = 'OPS'), '2300', 'GST Output', 'LIABILITY', 0.00),
    (4000, (SELECT id FROM companies WHERE code = 'OPS'), '4000', 'Revenue', 'REVENUE', 0.00),
    (5000, (SELECT id FROM companies WHERE code = 'OPS'), '5000', 'Cost of Goods Sold', 'COGS', 0.00),
    (6000, (SELECT id FROM companies WHERE code = 'OPS'), '6000', 'Operating Expenses', 'EXPENSE', 0.00),
    (6100, (SELECT id FROM companies WHERE code = 'OPS'), '6100', 'Discounts', 'EXPENSE', 0.00),
    (7000, (SELECT id FROM companies WHERE code = 'OPS'), '7000', 'Payroll Expense', 'EXPENSE', 0.00)
) AS v(id, company_id, code, name, type, balance)
ON CONFLICT (company_id, code) DO NOTHING;

UPDATE companies
SET default_inventory_account_id = 1200,
    default_cogs_account_id = 5000,
    default_revenue_account_id = 4000,
    default_tax_account_id = 2100,
    default_discount_account_id = 6100,
    gst_input_tax_account_id = 2200,
    gst_output_tax_account_id = 2300,
    gst_payable_account_id = 2100,
    payroll_expense_account_id = 7000,
    payroll_cash_account_id = 1000
WHERE code = 'OPS';

SELECT setval('accounts_id_seq', 7000, true);
