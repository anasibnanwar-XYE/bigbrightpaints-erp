-- CODE-RED: prevent multiple slips from sharing the same journal entry references
ALTER TABLE packaging_slips
    ADD CONSTRAINT uk_packaging_slips_journal_entry_id UNIQUE (journal_entry_id);

ALTER TABLE packaging_slips
    ADD CONSTRAINT uk_packaging_slips_cogs_journal_entry_id UNIQUE (cogs_journal_entry_id);
