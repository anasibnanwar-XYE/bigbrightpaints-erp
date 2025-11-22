-- Add outstanding tracking for invoices and raw material purchases (open-item AR/AP)

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS outstanding_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

UPDATE invoices SET outstanding_amount = total_amount WHERE total_amount IS NOT NULL;

ALTER TABLE raw_material_purchases
    ADD COLUMN IF NOT EXISTS outstanding_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

UPDATE raw_material_purchases SET outstanding_amount = total_amount WHERE total_amount IS NOT NULL;
