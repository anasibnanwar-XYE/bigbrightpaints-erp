CREATE TABLE tenant_usage_rollups (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    company_code VARCHAR(64) NOT NULL,
    dimension VARCHAR(32) NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_timezone VARCHAR(64) NOT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    usage_bytes BIGINT NOT NULL DEFAULT 0,
    source VARCHAR(32) NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tenant_usage_rollups_company
        FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_tenant_usage_rollups_window
        UNIQUE (company_id, dimension, period_type, period_start_at),
    CONSTRAINT chk_tenant_usage_rollups_dimension
        CHECK (dimension IN ('USERS', 'STORAGE', 'API_CALLS', 'PDF_EXPORTS', 'EMAILS', 'JOBS')),
    CONSTRAINT chk_tenant_usage_rollups_period_type
        CHECK (period_type IN ('DAILY', 'MONTHLY')),
    CONSTRAINT chk_tenant_usage_rollups_source
        CHECK (source IN ('SNAPSHOT', 'COUNTER')),
    CONSTRAINT chk_tenant_usage_rollups_non_negative
        CHECK (usage_count >= 0 AND usage_bytes >= 0),
    CONSTRAINT chk_tenant_usage_rollups_closed_at
        CHECK ((closed = FALSE AND closed_at IS NULL) OR (closed = TRUE AND closed_at IS NOT NULL))
);

CREATE INDEX idx_tenant_usage_rollups_company_period
    ON tenant_usage_rollups (company_id, period_type, period_start_at DESC);

CREATE INDEX idx_tenant_usage_rollups_dimension_period
    ON tenant_usage_rollups (dimension, period_type, period_start_at DESC);

CREATE INDEX idx_tenant_usage_rollups_closed
    ON tenant_usage_rollups (company_id, closed, period_end_at);
