-- V43: Link journal entries to accounting periods
-- Adds nullable foreign key column and supporting index so postings can be tied to periods

ALTER TABLE journal_entries
    ADD COLUMN IF NOT EXISTS accounting_period_id BIGINT REFERENCES accounting_periods(id);

CREATE INDEX IF NOT EXISTS idx_journal_entries_period
    ON journal_entries(accounting_period_id);
