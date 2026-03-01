ALTER TABLE sales_orders
    ALTER COLUMN status TYPE VARCHAR(32);

CREATE TABLE IF NOT EXISTS sales_order_status_history (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    sales_order_id BIGINT NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64),
    reason TEXT,
    changed_by VARCHAR(255) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sales_order_status_history_order_changed_at
    ON sales_order_status_history (sales_order_id, changed_at ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_sales_order_status_history_company_order
    ON sales_order_status_history (company_id, sales_order_id);

CREATE INDEX IF NOT EXISTS idx_sales_orders_company_order_number_created_at
    ON sales_orders (company_id, order_number, created_at DESC, id DESC);
