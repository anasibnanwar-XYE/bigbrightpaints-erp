CREATE TABLE super_admin_plan_templates (
    id BIGSERIAL PRIMARY KEY,
    stable_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    template_version INTEGER NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_until TIMESTAMP WITH TIME ZONE,
    cadence VARCHAR(32) NOT NULL,
    price_minor_units BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    trial_duration_days INTEGER NOT NULL,
    support_tier VARCHAR(32) NOT NULL,
    feature_flags JSONB NOT NULL DEFAULT '{}'::jsonb,
    max_active_users BIGINT NOT NULL,
    max_api_requests BIGINT NOT NULL,
    max_storage_bytes BIGINT NOT NULL,
    max_pdf_exports BIGINT NOT NULL,
    max_emails BIGINT NOT NULL,
    max_jobs BIGINT NOT NULL,
    burst_requests_per_minute BIGINT NOT NULL,
    max_concurrent_requests BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    archived_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_super_admin_plan_templates_stable_version
        UNIQUE (stable_id, template_version),
    CONSTRAINT chk_super_admin_plan_templates_status
        CHECK (status IN ('ACTIVE', 'SCHEDULED', 'ARCHIVED')),
    CONSTRAINT chk_super_admin_plan_templates_cadence
        CHECK (cadence IN ('MONTHLY', 'ANNUAL', 'CUSTOM')),
    CONSTRAINT chk_super_admin_plan_templates_support_tier
        CHECK (support_tier IN ('STANDARD', 'PRIORITY', 'DEDICATED')),
    CONSTRAINT chk_super_admin_plan_templates_non_negative_limits
        CHECK (
            price_minor_units >= 0
            AND trial_duration_days >= 0
            AND max_active_users >= 0
            AND max_api_requests >= 0
            AND max_storage_bytes >= 0
            AND max_pdf_exports >= 0
            AND max_emails >= 0
            AND max_jobs >= 0
            AND burst_requests_per_minute >= 0
            AND max_concurrent_requests >= 0
        )
);

CREATE INDEX idx_super_admin_plan_templates_stable_id
    ON super_admin_plan_templates (stable_id);

CREATE INDEX idx_super_admin_plan_templates_status
    ON super_admin_plan_templates (status);

INSERT INTO super_admin_plan_templates (
    stable_id,
    display_name,
    status,
    template_version,
    effective_from,
    cadence,
    price_minor_units,
    currency,
    trial_duration_days,
    support_tier,
    feature_flags,
    max_active_users,
    max_api_requests,
    max_storage_bytes,
    max_pdf_exports,
    max_emails,
    max_jobs,
    burst_requests_per_minute,
    max_concurrent_requests
) VALUES
    (
        'TRIAL',
        'Trial',
        'ACTIVE',
        1,
        TIMESTAMPTZ '2026-01-01T00:00:00Z',
        'MONTHLY',
        0,
        'INR',
        14,
        'STANDARD',
        '{"ACCOUNTING": true, "SALES": true, "INVENTORY": true, "PORTAL": true}'::jsonb,
        5,
        10000,
        1073741824,
        100,
        500,
        100,
        60,
        5
    ),
    (
        'STARTER',
        'Starter',
        'ACTIVE',
        1,
        TIMESTAMPTZ '2026-01-01T00:00:00Z',
        'MONTHLY',
        499900,
        'INR',
        0,
        'STANDARD',
        '{"ACCOUNTING": true, "SALES": true, "INVENTORY": true, "PORTAL": true, "REPORTS": true}'::jsonb,
        10,
        50000,
        5368709120,
        1000,
        2000,
        500,
        120,
        10
    ),
    (
        'GROWTH',
        'Growth',
        'ACTIVE',
        1,
        TIMESTAMPTZ '2026-01-01T00:00:00Z',
        'MONTHLY',
        1499900,
        'INR',
        0,
        'PRIORITY',
        '{"ACCOUNTING": true, "SALES": true, "INVENTORY": true, "PURCHASING": true, "PRODUCTION": true, "REPORTS": true, "PORTAL": true}'::jsonb,
        50,
        250000,
        53687091200,
        10000,
        20000,
        5000,
        300,
        25
    ),
    (
        'ENTERPRISE',
        'Enterprise',
        'ACTIVE',
        1,
        TIMESTAMPTZ '2026-01-01T00:00:00Z',
        'MONTHLY',
        0,
        'INR',
        0,
        'DEDICATED',
        '{"ACCOUNTING": true, "SALES": true, "INVENTORY": true, "PURCHASING": true, "PRODUCTION": true, "HR": true, "REPORTS": true, "PORTAL": true}'::jsonb,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0
    ),
    (
        'CUSTOM',
        'Custom',
        'ACTIVE',
        1,
        TIMESTAMPTZ '2026-01-01T00:00:00Z',
        'CUSTOM',
        0,
        'INR',
        0,
        'DEDICATED',
        '{"CUSTOM_PLAN": true}'::jsonb,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0
    );
