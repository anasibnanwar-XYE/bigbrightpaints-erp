-- Add idempotency markers to sales_orders to prevent double posting
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS sales_journal_entry_id BIGINT;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS cogs_journal_entry_id BIGINT;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS fulfillment_invoice_id BIGINT;

-- Create indexes for lookup
CREATE INDEX IF NOT EXISTS idx_sales_orders_sales_journal ON sales_orders(sales_journal_entry_id) WHERE sales_journal_entry_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sales_orders_cogs_journal ON sales_orders(cogs_journal_entry_id) WHERE cogs_journal_entry_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sales_orders_fulfillment_invoice ON sales_orders(fulfillment_invoice_id) WHERE fulfillment_invoice_id IS NOT NULL;

COMMENT ON COLUMN sales_orders.sales_journal_entry_id IS 'Idempotency marker: ID of sales/revenue journal entry';
COMMENT ON COLUMN sales_orders.cogs_journal_entry_id IS 'Idempotency marker: ID of COGS journal entry';
COMMENT ON COLUMN sales_orders.fulfillment_invoice_id IS 'Idempotency marker: ID of invoice issued during fulfillment';
