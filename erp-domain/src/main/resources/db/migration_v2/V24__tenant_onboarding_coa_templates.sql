CREATE TABLE IF NOT EXISTS public.coa_templates (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    account_count INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_coa_templates_code
    ON public.coa_templates (code);

INSERT INTO public.coa_templates (code, name, description, account_count, active)
VALUES
    ('INDIAN_STANDARD', 'Indian Standard', 'Ind AS aligned chart with assets, liabilities, equity, revenue, expenses, and Indian GST/TDS tax ledgers.', 75, TRUE),
    ('MANUFACTURING', 'Manufacturing', 'Extends Indian Standard with manufacturing coverage including WIP, COGS, raw material, finished goods, and factory overhead accounts.', 91, TRUE),
    ('GENERIC', 'Generic', 'Basic multi-purpose chart of accounts template for assets, liabilities, equity, revenue, expenses, and essential tax ledgers.', 63, TRUE)
ON CONFLICT (code)
DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    account_count = EXCLUDED.account_count,
    active = EXCLUDED.active;
