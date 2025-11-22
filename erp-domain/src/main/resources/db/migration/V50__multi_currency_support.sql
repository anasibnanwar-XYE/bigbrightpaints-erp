-- V50: Multi-currency scaffolding
-- Adds base_currency to companies and captures source currency on journal entries

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS base_currency VARCHAR(8) NOT NULL DEFAULT 'INR';

ALTER TABLE journal_entries
    ADD COLUMN IF NOT EXISTS currency VARCHAR(8) NOT NULL DEFAULT 'INR',
    ADD COLUMN IF NOT EXISTS fx_rate NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS foreign_amount_total NUMERIC(18,2);

-- Seed existing rows with defaults to keep legacy behavior
UPDATE journal_entries SET currency = 'INR' WHERE currency IS NULL;
