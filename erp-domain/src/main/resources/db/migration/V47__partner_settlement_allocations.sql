CREATE TABLE IF NOT EXISTS partner_settlement_allocations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    partner_type VARCHAR(16) NOT NULL,
    dealer_id BIGINT REFERENCES dealers(id) ON DELETE CASCADE,
    supplier_id BIGINT REFERENCES suppliers(id) ON DELETE CASCADE,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE SET NULL,
    purchase_id BIGINT REFERENCES raw_material_purchases(id) ON DELETE SET NULL,
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    settlement_date DATE NOT NULL,
    allocation_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    write_off_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    fx_difference_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'INR',
    memo TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_partner_settlement_partner CHECK (
        (partner_type = 'DEALER' AND dealer_id IS NOT NULL AND supplier_id IS NULL)
        OR (partner_type = 'SUPPLIER' AND supplier_id IS NOT NULL AND dealer_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_partner_settlement_company
    ON partner_settlement_allocations(company_id);
CREATE INDEX IF NOT EXISTS idx_partner_settlement_partner
    ON partner_settlement_allocations(company_id, partner_type, dealer_id, supplier_id);
CREATE INDEX IF NOT EXISTS idx_partner_settlement_invoice
    ON partner_settlement_allocations(company_id, invoice_id);
CREATE INDEX IF NOT EXISTS idx_partner_settlement_purchase
    ON partner_settlement_allocations(company_id, purchase_id);
