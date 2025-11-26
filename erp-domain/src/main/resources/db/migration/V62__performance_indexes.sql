-- Performance indexes for high-volume tables

-- Journal lines account index for ledger queries (MIG-002)
CREATE INDEX IF NOT EXISTS idx_journal_lines_account_id ON journal_lines(account_id);

-- Inventory quantity indexes for availability queries (MIG-001)
CREATE INDEX IF NOT EXISTS idx_finished_good_batches_quantity ON finished_good_batches(quantity_available);
CREATE INDEX IF NOT EXISTS idx_finished_goods_stock ON finished_goods(current_stock);
CREATE INDEX IF NOT EXISTS idx_raw_material_batches_quantity ON raw_material_batches(quantity);

-- Sales order lookup indexes
CREATE INDEX IF NOT EXISTS idx_sales_orders_dealer ON sales_orders(dealer_id);
CREATE INDEX IF NOT EXISTS idx_sales_orders_status ON sales_orders(status);
CREATE INDEX IF NOT EXISTS idx_sales_orders_created_at ON sales_orders(created_at);

-- Invoice lookup indexes
CREATE INDEX IF NOT EXISTS idx_invoices_dealer ON invoices(dealer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);

-- Ledger entry indexes for partner reconciliation
CREATE INDEX IF NOT EXISTS idx_dealer_ledger_entries_dealer ON dealer_ledger_entries(dealer_id);
CREATE INDEX IF NOT EXISTS idx_supplier_ledger_entries_supplier ON supplier_ledger_entries(supplier_id);
