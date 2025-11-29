-- V66: Unique constraint on journal_entries (company_id, reference_number) to prevent duplicates
ALTER TABLE journal_entries ADD CONSTRAINT uk_journal_company_reference UNIQUE (company_id, reference_number);
