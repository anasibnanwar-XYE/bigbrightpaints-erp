ALTER TABLE invoice_lines
    ADD COLUMN IF NOT EXISTS taxable_amount NUMERIC(18,2);

ALTER TABLE invoice_lines
    ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(18,2);

ALTER TABLE invoice_lines
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18,2);
