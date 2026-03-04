CREATE TABLE IF NOT EXISTS period_close_requests (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    accounting_period_id BIGINT NOT NULL REFERENCES accounting_periods(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(255) NOT NULL,
    request_note TEXT,
    force_requested BOOLEAN NOT NULL DEFAULT FALSE,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMPTZ,
    review_note TEXT,
    approval_note TEXT,
    CONSTRAINT chk_period_close_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_period_close_requests_company_public
    ON period_close_requests (company_id, public_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_period_close_requests_company_period_pending
    ON period_close_requests (company_id, accounting_period_id)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_period_close_requests_company_status_requested
    ON period_close_requests (company_id, status, requested_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_period_close_requests_company_period_requested
    ON period_close_requests (company_id, accounting_period_id, requested_at DESC, id DESC);
