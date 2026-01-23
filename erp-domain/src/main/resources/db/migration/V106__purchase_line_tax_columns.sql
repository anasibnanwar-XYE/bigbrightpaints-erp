ALTER TABLE raw_material_purchase_items
    ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(10,4);

ALTER TABLE raw_material_purchase_items
    ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(18,4);
