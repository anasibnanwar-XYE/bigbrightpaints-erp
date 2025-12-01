-- Add aging and payment tracking fields to dealer ledger for sub-ledger reports
-- Enables:
-- - Aged receivables reports (30/60/90 days)
-- - Payment matching
-- - DSO (Days Sales Outstanding) calculation
-- - Credit limit enforcement

ALTER TABLE dealer_ledger_entries ADD COLUMN due_date DATE;
ALTER TABLE dealer_ledger_entries ADD COLUMN paid_date DATE;
ALTER TABLE dealer_ledger_entries ADD COLUMN invoice_number VARCHAR(100);
ALTER TABLE dealer_ledger_entries ADD COLUMN payment_status VARCHAR(20) DEFAULT 'UNPAID';
ALTER TABLE dealer_ledger_entries ADD COLUMN amount_paid NUMERIC(19, 4) DEFAULT 0;

-- Indexes for aging queries
CREATE INDEX idx_dealer_ledger_due_date ON dealer_ledger_entries(company_id, due_date);
CREATE INDEX idx_dealer_ledger_status ON dealer_ledger_entries(company_id, payment_status);
CREATE INDEX idx_dealer_ledger_invoice ON dealer_ledger_entries(company_id, invoice_number);

-- Same for supplier ledger
ALTER TABLE supplier_ledger_entries ADD COLUMN due_date DATE;
ALTER TABLE supplier_ledger_entries ADD COLUMN paid_date DATE;
ALTER TABLE supplier_ledger_entries ADD COLUMN invoice_number VARCHAR(100);
ALTER TABLE supplier_ledger_entries ADD COLUMN payment_status VARCHAR(20) DEFAULT 'UNPAID';
ALTER TABLE supplier_ledger_entries ADD COLUMN amount_paid NUMERIC(19, 4) DEFAULT 0;

CREATE INDEX idx_supplier_ledger_due_date ON supplier_ledger_entries(company_id, due_date);
CREATE INDEX idx_supplier_ledger_status ON supplier_ledger_entries(company_id, payment_status);

COMMENT ON COLUMN dealer_ledger_entries.due_date IS 'Invoice due date for aging calculations';
COMMENT ON COLUMN dealer_ledger_entries.payment_status IS 'UNPAID, PARTIAL, or PAID';
COMMENT ON COLUMN dealer_ledger_entries.amount_paid IS 'Amount paid against this entry (for partial payments)';
