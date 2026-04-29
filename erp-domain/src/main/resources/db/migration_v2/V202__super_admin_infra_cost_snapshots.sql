CREATE TABLE IF NOT EXISTS super_admin_infra_cost_snapshots (
    id BIGSERIAL PRIMARY KEY,
    component VARCHAR(32) NOT NULL,
    period_start_at TIMESTAMPTZ NOT NULL,
    period_end_at TIMESTAMPTZ NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source VARCHAR(120) NOT NULL,
    notes VARCHAR(300),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    entered_by VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ,
    correction_count INTEGER NOT NULL DEFAULT 0,
    audit_event_id BIGINT,
    CONSTRAINT chk_super_admin_infra_cost_component
        CHECK (component IN ('APP_SERVER', 'DATABASE', 'STORAGE', 'EMAIL', 'BACKUP', 'MONITORING')),
    CONSTRAINT chk_super_admin_infra_cost_period
        CHECK (period_end_at > period_start_at),
    CONSTRAINT chk_super_admin_infra_cost_amount_non_negative
        CHECK (amount_minor_units >= 0),
    CONSTRAINT chk_super_admin_infra_cost_currency_upper
        CHECK (currency = upper(currency) AND length(currency) = 3),
    CONSTRAINT chk_super_admin_infra_cost_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_super_admin_infra_cost_correction_count
        CHECK (correction_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_super_admin_infra_cost_component
    ON super_admin_infra_cost_snapshots(component, status);

CREATE INDEX IF NOT EXISTS idx_super_admin_infra_cost_period
    ON super_admin_infra_cost_snapshots(currency, period_end_at DESC, status);

CREATE TABLE IF NOT EXISTS super_admin_infra_cost_snapshot_corrections (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES super_admin_infra_cost_snapshots(id) ON DELETE RESTRICT,
    previous_amount_minor_units BIGINT NOT NULL,
    new_amount_minor_units BIGINT NOT NULL,
    previous_currency VARCHAR(3) NOT NULL,
    new_currency VARCHAR(3) NOT NULL,
    reason VARCHAR(300) NOT NULL,
    corrected_by VARCHAR(160),
    corrected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    audit_event_id BIGINT,
    CONSTRAINT chk_super_admin_infra_cost_correction_amounts_non_negative
        CHECK (previous_amount_minor_units >= 0 AND new_amount_minor_units >= 0),
    CONSTRAINT chk_super_admin_infra_cost_correction_currency_upper
        CHECK (
            previous_currency = upper(previous_currency)
            AND length(previous_currency) = 3
            AND new_currency = upper(new_currency)
            AND length(new_currency) = 3
        ),
    CONSTRAINT chk_super_admin_infra_cost_correction_reason_present
        CHECK (length(trim(reason)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_super_admin_infra_cost_corrections_snapshot
    ON super_admin_infra_cost_snapshot_corrections(snapshot_id, corrected_at DESC, id DESC);
