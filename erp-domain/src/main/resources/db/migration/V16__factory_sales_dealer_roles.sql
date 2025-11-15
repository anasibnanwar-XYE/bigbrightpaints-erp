-- Factory, Sales, Dealer roles and permissions

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

-- Attach permissions to roles
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

-- Ensure admins can create dealer roles as well
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
