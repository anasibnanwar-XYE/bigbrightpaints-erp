-- Adds lock/reopen audit metadata and closing journal link on accounting periods
ALTER TABLE accounting_periods
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS locked_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS lock_reason TEXT,
    ADD COLUMN IF NOT EXISTS reopened_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reopened_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS reopen_reason TEXT,
    ADD COLUMN IF NOT EXISTS closing_journal_entry_id BIGINT REFERENCES journal_entries(id);

-- No data backfill required; defaults are NULL, status remains OPEN/CLOSED for historical rows.
