ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS billing_plan_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS billing_plan_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS billing_plan_currency VARCHAR(16) NOT NULL DEFAULT 'INR',
    ADD COLUMN IF NOT EXISTS billing_plan_monthly_rate NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS billing_plan_annual_rate NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS billing_plan_seats BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS billing_plan_updated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS billing_plan_updated_by VARCHAR(255);

UPDATE companies
SET billing_plan_currency = 'INR'
WHERE billing_plan_currency IS NULL OR btrim(billing_plan_currency) = '';

UPDATE companies
SET billing_plan_monthly_rate = 0
WHERE billing_plan_monthly_rate IS NULL OR billing_plan_monthly_rate < 0;

UPDATE companies
SET billing_plan_annual_rate = 0
WHERE billing_plan_annual_rate IS NULL OR billing_plan_annual_rate < 0;

UPDATE companies
SET billing_plan_seats = 0
WHERE billing_plan_seats IS NULL OR billing_plan_seats < 0;

ALTER TABLE companies
    DROP CONSTRAINT IF EXISTS chk_companies_billing_plan_monthly_rate_non_negative;

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_billing_plan_monthly_rate_non_negative
        CHECK (billing_plan_monthly_rate >= 0);

ALTER TABLE companies
    DROP CONSTRAINT IF EXISTS chk_companies_billing_plan_annual_rate_non_negative;

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_billing_plan_annual_rate_non_negative
        CHECK (billing_plan_annual_rate >= 0);

ALTER TABLE companies
    DROP CONSTRAINT IF EXISTS chk_companies_billing_plan_seats_non_negative;

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_billing_plan_seats_non_negative
        CHECK (billing_plan_seats >= 0);
