-- Seed dev data for ERP Domain (run after app has created schema via Flyway)
-- Safe to re-run: uses ON CONFLICT DO NOTHING where applicable

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Ensure base role exists
INSERT INTO roles(name, description)
VALUES ('ROLE_ADMIN', 'Administrator')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles(name, description)
VALUES ('ROLE_ACCOUNTING', 'Accounting operator')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions(code, description)
VALUES ('portal:accounting', 'Access to the accounting operator portal')
ON CONFLICT (code) DO NOTHING;

WITH role_row AS (
    SELECT id FROM roles WHERE name = 'ROLE_ACCOUNTING'
),
perm_row AS (
    SELECT id FROM permissions WHERE code = 'portal:accounting'
)
INSERT INTO role_permissions(role_id, permission_id)
SELECT role_row.id, perm_row.id
FROM role_row, perm_row
ON CONFLICT DO NOTHING;

-- Seed factory, sales, dealer roles and permissions
INSERT INTO permissions(code, description)
VALUES
    ('portal:factory', 'Access to the factory control portal'),
    ('portal:sales', 'Access to the sales console'),
    ('portal:dealer', 'Access to dealer workspace'),
    ('rbac:dealer-role:create', 'Ability to create dealer specific roles')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(name, description)
VALUES
    ('ROLE_FACTORY_MANAGER', 'Factory floor supervisor'),
    ('ROLE_SALES_MANAGER', 'Sales and dealers operator'),
    ('ROLE_DEALER_OPERATOR', 'Dealer workspace user')
ON CONFLICT (name) DO NOTHING;

WITH factory_role AS (
    SELECT id FROM roles WHERE name = 'ROLE_FACTORY_MANAGER'
),
factory_perm AS (
    SELECT id FROM permissions WHERE code = 'portal:factory'
)
INSERT INTO role_permissions(role_id, permission_id)
SELECT factory_role.id, factory_perm.id
FROM factory_role, factory_perm
ON CONFLICT DO NOTHING;

WITH sales_role AS (
    SELECT id FROM roles WHERE name = 'ROLE_SALES_MANAGER'
),
sales_perms AS (
    SELECT id FROM permissions WHERE code IN ('portal:sales', 'rbac:dealer-role:create')
)
INSERT INTO role_permissions(role_id, permission_id)
SELECT sales_role.id, sales_perms.id
FROM sales_role, sales_perms
ON CONFLICT DO NOTHING;

WITH dealer_role AS (
    SELECT id FROM roles WHERE name = 'ROLE_DEALER_OPERATOR'
),
dealer_perm AS (
    SELECT id FROM permissions WHERE code = 'portal:dealer'
)
INSERT INTO role_permissions(role_id, permission_id)
SELECT dealer_role.id, dealer_perm.id
FROM dealer_role, dealer_perm
ON CONFLICT DO NOTHING;

-- allow admins to create dealer roles
WITH admin_role AS (
    SELECT id FROM roles WHERE name = 'ROLE_ADMIN'
),
dealer_create_perm AS (
    SELECT id FROM permissions WHERE code = 'rbac:dealer-role:create'
)
INSERT INTO role_permissions(role_id, permission_id)
SELECT admin_role.id, dealer_create_perm.id
FROM admin_role, dealer_create_perm
ON CONFLICT DO NOTHING;

-- Company
INSERT INTO companies(name, code, timezone)
VALUES ('Acme Corp', 'ACME', 'UTC')
ON CONFLICT (code) DO NOTHING;

-- User with bcrypt password
INSERT INTO app_users(email, password_hash, display_name, enabled)
VALUES ('admin@bbp.com', crypt('admin123', gen_salt('bf')), 'Admin', true)
ON CONFLICT (email) DO NOTHING;

-- Link user to ROLE_ADMIN
WITH r AS (
    SELECT id FROM roles WHERE name = 'ROLE_ADMIN'
), u AS (
    SELECT id FROM app_users WHERE email = 'admin@bbp.com'
)
INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id FROM u, r
ON CONFLICT DO NOTHING;

-- Link user to ACME company
WITH c AS (
    SELECT id FROM companies WHERE code = 'ACME'
), u AS (
    SELECT id FROM app_users WHERE email = 'admin@bbp.com'
)
INSERT INTO user_companies(user_id, company_id)
SELECT u.id, c.id FROM u, c
ON CONFLICT DO NOTHING;
