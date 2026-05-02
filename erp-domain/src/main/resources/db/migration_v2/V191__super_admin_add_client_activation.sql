ALTER TABLE companies
    ADD COLUMN commercial_plan_id VARCHAR(64) NOT NULL DEFAULT 'TRIAL',
    ADD COLUMN commercial_billing_status VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN commercial_trial_ends_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN commercial_support_tier VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN activation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SENT',
    ADD COLUMN activation_sent_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN activation_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_activation_status_v191
        CHECK (activation_status IN ('NOT_SENT', 'SENT', 'EXPIRED', 'USED', 'SUPERSEDED')),
    ADD CONSTRAINT chk_companies_commercial_billing_status_v191
        CHECK (commercial_billing_status IN ('TRIAL', 'MANUAL', 'PAID', 'DUE', 'OVERDUE', 'GRACE', 'CANCELED', 'ARCHIVED'));

CREATE TABLE tenant_activation_tokens (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_digest VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    used_at TIMESTAMP WITH TIME ZONE,
    expired_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_tenant_activation_tokens_token_digest UNIQUE (token_digest),
    CONSTRAINT chk_tenant_activation_tokens_token_digest_hex
        CHECK (token_digest ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_tenant_activation_tokens_status
        CHECK (status IN ('ISSUED', 'SENT', 'USED', 'EXPIRED', 'SUPERSEDED'))
);

CREATE INDEX idx_tenant_activation_tokens_company_owner
    ON tenant_activation_tokens (company_id, owner_user_id, status, created_at DESC);
