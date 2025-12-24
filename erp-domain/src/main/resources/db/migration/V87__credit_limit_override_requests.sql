CREATE TABLE IF NOT EXISTS credit_limit_override_requests (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    dealer_id BIGINT REFERENCES dealers(id) ON DELETE SET NULL,
    packaging_slip_id BIGINT REFERENCES packaging_slips(id) ON DELETE SET NULL,
    sales_order_id BIGINT REFERENCES sales_orders(id) ON DELETE SET NULL,
    dispatch_amount NUMERIC(18,2) NOT NULL,
    current_exposure NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit_limit NUMERIC(18,2) NOT NULL DEFAULT 0,
    required_headroom NUMERIC(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    reason TEXT,
    requested_by VARCHAR(255),
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_credit_override_company_status
    ON credit_limit_override_requests(company_id, status);

CREATE INDEX IF NOT EXISTS idx_credit_override_packaging_slip
    ON credit_limit_override_requests(packaging_slip_id);

CREATE INDEX IF NOT EXISTS idx_credit_override_sales_order
    ON credit_limit_override_requests(sales_order_id);
