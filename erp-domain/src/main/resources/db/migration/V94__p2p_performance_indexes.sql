-- V94: P2P performance indexes for supplier lists and purchase history

CREATE INDEX IF NOT EXISTS idx_suppliers_company_name
    ON suppliers (company_id, name);

CREATE INDEX IF NOT EXISTS idx_raw_material_purchases_company_date
    ON raw_material_purchases (company_id, invoice_date DESC);
