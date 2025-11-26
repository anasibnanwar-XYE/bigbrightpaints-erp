-- Dispatch confirmation workflow support

-- Add new columns to packaging_slips
ALTER TABLE packaging_slips ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;
ALTER TABLE packaging_slips ADD COLUMN IF NOT EXISTS confirmed_by VARCHAR(255);
ALTER TABLE packaging_slips ADD COLUMN IF NOT EXISTS dispatch_notes VARCHAR(1000);
ALTER TABLE packaging_slips ADD COLUMN IF NOT EXISTS journal_entry_id BIGINT REFERENCES journal_entries(id);
ALTER TABLE packaging_slips ADD COLUMN IF NOT EXISTS cogs_journal_entry_id BIGINT REFERENCES journal_entries(id);

-- Add new columns to packaging_slip_lines
ALTER TABLE packaging_slip_lines ADD COLUMN IF NOT EXISTS ordered_quantity DECIMAL(19,4);
ALTER TABLE packaging_slip_lines ADD COLUMN IF NOT EXISTS shipped_quantity DECIMAL(19,4);
ALTER TABLE packaging_slip_lines ADD COLUMN IF NOT EXISTS backorder_quantity DECIMAL(19,4);
ALTER TABLE packaging_slip_lines ADD COLUMN IF NOT EXISTS notes VARCHAR(500);

-- Backfill ordered_quantity from quantity for existing records
UPDATE packaging_slip_lines SET ordered_quantity = quantity WHERE ordered_quantity IS NULL;

-- Make ordered_quantity NOT NULL after backfill
ALTER TABLE packaging_slip_lines ALTER COLUMN ordered_quantity SET NOT NULL;

-- Add index for status filtering
CREATE INDEX IF NOT EXISTS idx_packaging_slips_status ON packaging_slips(status);
CREATE INDEX IF NOT EXISTS idx_packaging_slips_confirmed_at ON packaging_slips(confirmed_at);
