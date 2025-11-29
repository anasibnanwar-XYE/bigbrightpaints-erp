-- Add supporting indexes for frequent inventory/accounting lookups

-- Journal lines by account for ledger queries
CREATE INDEX IF NOT EXISTS idx_journal_lines_account_id ON journal_lines(account_id);

-- Inventory availability lookups
CREATE INDEX IF NOT EXISTS idx_finished_goods_current_stock ON finished_goods(current_stock);
CREATE INDEX IF NOT EXISTS idx_finished_goods_reserved_stock ON finished_goods(reserved_stock);
CREATE INDEX IF NOT EXISTS idx_finished_good_batches_qty_available ON finished_good_batches(quantity_available);
