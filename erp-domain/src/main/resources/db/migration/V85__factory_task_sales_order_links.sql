-- Add links from factory tasks to sales order and packaging slip for shortage tracking/cancellation
ALTER TABLE factory_tasks
    ADD COLUMN IF NOT EXISTS sales_order_id BIGINT,
    ADD COLUMN IF NOT EXISTS packaging_slip_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_factory_tasks_sales_order_id
    ON factory_tasks(sales_order_id);
