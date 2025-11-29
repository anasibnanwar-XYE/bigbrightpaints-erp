-- V67: Indexes for journal query performance
CREATE INDEX IF NOT EXISTS idx_journal_company_date_status ON journal_entries (company_id, entry_date DESC, status);
