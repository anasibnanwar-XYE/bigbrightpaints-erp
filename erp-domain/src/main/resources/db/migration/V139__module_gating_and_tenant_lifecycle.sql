ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS enabled_modules JSONB;

UPDATE companies
SET enabled_modules = '["MANUFACTURING","HR_PAYROLL","PURCHASING","PORTAL","REPORTS_ADVANCED"]'::jsonb
WHERE enabled_modules IS NULL;

ALTER TABLE companies
    ALTER COLUMN enabled_modules SET DEFAULT '["MANUFACTURING","HR_PAYROLL","PURCHASING","PORTAL","REPORTS_ADVANCED"]'::jsonb;

ALTER TABLE companies
    ALTER COLUMN enabled_modules SET NOT NULL;

UPDATE companies
SET lifecycle_state = 'SUSPENDED'
WHERE lifecycle_state = 'HOLD';

UPDATE companies
SET lifecycle_state = 'DEACTIVATED'
WHERE lifecycle_state = 'BLOCKED';

ALTER TABLE companies
    DROP CONSTRAINT IF EXISTS chk_companies_lifecycle_state;

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_lifecycle_state
    CHECK (lifecycle_state IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED'));
