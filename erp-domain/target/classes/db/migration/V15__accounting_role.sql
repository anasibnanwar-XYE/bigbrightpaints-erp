-- Seed accounting portal role + permission

INSERT INTO permissions(code, description)
VALUES ('portal:accounting', 'Access to the accounting operator portal')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(name, description)
VALUES ('ROLE_ACCOUNTING', 'Accounting operator')
ON CONFLICT (name) DO NOTHING;

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
