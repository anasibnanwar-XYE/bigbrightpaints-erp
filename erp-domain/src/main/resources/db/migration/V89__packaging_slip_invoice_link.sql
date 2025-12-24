ALTER TABLE packaging_slips
    ADD COLUMN IF NOT EXISTS invoice_id BIGINT;

ALTER TABLE packaging_slips
    ADD CONSTRAINT fk_packaging_slips_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
    ON DELETE SET NULL;
