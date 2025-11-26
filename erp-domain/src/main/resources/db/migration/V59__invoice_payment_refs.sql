-- Migration to create invoice_payment_refs table for Invoice.paymentReferences ElementCollection
CREATE TABLE IF NOT EXISTS invoice_payment_refs (
    invoice_id BIGINT NOT NULL,
    payment_reference VARCHAR(255) NOT NULL,
    PRIMARY KEY (invoice_id, payment_reference),
    CONSTRAINT fk_invoice_payment_refs_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_invoice_payment_refs_invoice_id ON invoice_payment_refs(invoice_id);
