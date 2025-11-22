-- Add idempotency key to sales orders with a partial unique index
ALTER TABLE sales_orders
    ADD COLUMN idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX idx_sales_orders_idempotency
    ON sales_orders(company_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
