CREATE TABLE IF NOT EXISTS reconciliation_discrepancies (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    accounting_period_id BIGINT REFERENCES accounting_periods(id) ON DELETE SET NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    discrepancy_type VARCHAR(32) NOT NULL,
    partner_type VARCHAR(32),
    partner_id BIGINT,
    partner_code VARCHAR(128),
    partner_name VARCHAR(255),
    expected_amount NUMERIC(19,2) NOT NULL,
    actual_amount NUMERIC(19,2) NOT NULL,
    variance NUMERIC(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    resolution VARCHAR(32),
    resolution_note TEXT,
    resolution_journal_id BIGINT REFERENCES journal_entries(id) ON DELETE SET NULL,
    resolved_by VARCHAR(255),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_recon_discrepancy_type
        CHECK (discrepancy_type IN ('AR', 'AP', 'INVENTORY', 'GST')),
    CONSTRAINT chk_recon_discrepancy_partner_type
        CHECK (partner_type IS NULL OR partner_type IN ('DEALER', 'SUPPLIER')),
    CONSTRAINT chk_recon_discrepancy_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'ADJUSTED', 'RESOLVED')),
    CONSTRAINT chk_recon_discrepancy_resolution
        CHECK (resolution IS NULL OR resolution IN ('ACKNOWLEDGED', 'ADJUSTMENT_JOURNAL', 'WRITE_OFF')),
    CONSTRAINT chk_recon_discrepancy_period_window
        CHECK (period_end >= period_start)
);

CREATE INDEX IF NOT EXISTS idx_recon_disc_company_period_status
    ON reconciliation_discrepancies (company_id, accounting_period_id, status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_recon_disc_company_type_period
    ON reconciliation_discrepancies (company_id, discrepancy_type, period_start, period_end, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_recon_disc_company_created
    ON reconciliation_discrepancies (company_id, created_at DESC, id DESC);
