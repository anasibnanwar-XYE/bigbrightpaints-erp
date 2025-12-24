CREATE UNIQUE INDEX IF NOT EXISTS uq_packaging_slips_invoice_id
    ON packaging_slips(invoice_id)
    WHERE invoice_id IS NOT NULL;
