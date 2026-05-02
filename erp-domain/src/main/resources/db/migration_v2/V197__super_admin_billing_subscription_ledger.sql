CREATE TABLE IF NOT EXISTS super_admin_billing_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    plan_id VARCHAR(64) NOT NULL,
    custom_plan_name VARCHAR(160),
    status VARCHAR(32) NOT NULL,
    cadence VARCHAR(32) NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    collection_mode VARCHAR(32) NOT NULL,
    period_start_at TIMESTAMPTZ NOT NULL,
    period_end_at TIMESTAMPTZ,
    renewal_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ,
    trial_start_at TIMESTAMPTZ,
    trial_end_at TIMESTAMPTZ,
    grace_until_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    external_reference VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    audit_event_id BIGINT,
    CONSTRAINT chk_super_admin_billing_subscription_amount_non_negative
        CHECK (amount_minor_units >= 0),
    CONSTRAINT chk_super_admin_billing_subscription_currency_upper
        CHECK (currency = upper(currency) AND length(currency) = 3),
    CONSTRAINT chk_super_admin_billing_subscription_status
        CHECK (status IN ('TRIAL', 'MANUAL', 'ACTIVE', 'CANCELED', 'ARCHIVED')),
    CONSTRAINT chk_super_admin_billing_subscription_cadence
        CHECK (cadence IN ('MONTHLY', 'ANNUAL', 'CUSTOM')),
    CONSTRAINT chk_super_admin_billing_subscription_collection_mode
        CHECK (collection_mode IN ('MANUAL', 'EXTERNAL', 'OFFLINE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_super_admin_billing_subscription_one_active
    ON super_admin_billing_subscriptions(company_id)
    WHERE status IN ('TRIAL', 'MANUAL', 'ACTIVE');

CREATE INDEX IF NOT EXISTS idx_super_admin_billing_subscriptions_company
    ON super_admin_billing_subscriptions(company_id);

CREATE INDEX IF NOT EXISTS idx_super_admin_billing_subscriptions_status_currency
    ON super_admin_billing_subscriptions(status, currency);

CREATE TABLE IF NOT EXISTS super_admin_billing_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    subscription_id BIGINT NOT NULL REFERENCES super_admin_billing_subscriptions(id)
        ON DELETE RESTRICT,
    entry_type VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason VARCHAR(300) NOT NULL,
    external_reference VARCHAR(160),
    idempotency_key VARCHAR(160) NOT NULL,
    balance_before_minor_units BIGINT NOT NULL,
    balance_after_minor_units BIGINT NOT NULL,
    billing_status_after VARCHAR(32) NOT NULL,
    created_by VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    audit_event_id BIGINT,
    CONSTRAINT chk_super_admin_billing_ledger_amount_positive
        CHECK (amount_minor_units > 0),
    CONSTRAINT chk_super_admin_billing_ledger_currency_upper
        CHECK (currency = upper(currency) AND length(currency) = 3),
    CONSTRAINT chk_super_admin_billing_ledger_type
        CHECK (entry_type IN ('INVOICE', 'PAYMENT', 'ADJUSTMENT')),
    CONSTRAINT chk_super_admin_billing_ledger_direction
        CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_super_admin_billing_ledger_reason_present
        CHECK (length(trim(reason)) > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_super_admin_billing_ledger_idempotency
    ON super_admin_billing_ledger_entries(company_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_super_admin_billing_ledger_company_created
    ON super_admin_billing_ledger_entries(company_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_super_admin_billing_ledger_subscription
    ON super_admin_billing_ledger_entries(subscription_id);
