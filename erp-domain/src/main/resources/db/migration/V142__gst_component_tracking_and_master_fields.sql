ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS state_code VARCHAR(2);

ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS gst_number VARCHAR(15);

ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS state_code VARCHAR(2);

ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS gst_registration_type VARCHAR(32) NOT NULL DEFAULT 'UNREGISTERED';

ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS gst_number VARCHAR(15);

ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS state_code VARCHAR(2);

ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS gst_registration_type VARCHAR(32) NOT NULL DEFAULT 'UNREGISTERED';

ALTER TABLE invoice_lines
    ADD COLUMN IF NOT EXISTS cgst_amount NUMERIC(18,4);

ALTER TABLE invoice_lines
    ADD COLUMN IF NOT EXISTS sgst_amount NUMERIC(18,4);

ALTER TABLE invoice_lines
    ADD COLUMN IF NOT EXISTS igst_amount NUMERIC(18,4);

ALTER TABLE raw_material_purchase_items
    ADD COLUMN IF NOT EXISTS cgst_amount NUMERIC(18,4);

ALTER TABLE raw_material_purchase_items
    ADD COLUMN IF NOT EXISTS sgst_amount NUMERIC(18,4);

ALTER TABLE raw_material_purchase_items
    ADD COLUMN IF NOT EXISTS igst_amount NUMERIC(18,4);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_dealers_gst_registration_type'
    ) THEN
        ALTER TABLE dealers
            ADD CONSTRAINT chk_dealers_gst_registration_type
            CHECK (gst_registration_type IN ('REGULAR', 'COMPOSITION', 'UNREGISTERED'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_suppliers_gst_registration_type'
    ) THEN
        ALTER TABLE suppliers
            ADD CONSTRAINT chk_suppliers_gst_registration_type
            CHECK (gst_registration_type IN ('REGULAR', 'COMPOSITION', 'UNREGISTERED'));
    END IF;
END $$;
