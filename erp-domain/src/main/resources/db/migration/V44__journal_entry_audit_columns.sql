-- V44: Bring journal_entries table in sync with entity audit fields
-- Adds missing metadata columns so posting, reversal, and correction flows
-- can persist responsible users and linkage

ALTER TABLE journal_entries
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS posted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS posted_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS void_reason TEXT,
    ADD COLUMN IF NOT EXISTS voided_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS correction_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS correction_reason TEXT,
    ADD COLUMN IF NOT EXISTS reversal_of_id BIGINT REFERENCES journal_entries(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_journal_entries_reversal
    ON journal_entries (reversal_of_id);

-- Ensure legacy rows have a deterministic updated_at for NOT NULL constraint
UPDATE journal_entries
   SET updated_at = COALESCE(updated_at, created_at, NOW())
 WHERE updated_at IS NULL;
