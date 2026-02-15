-- M16-S4 accounting audit read-model hotspot indexes.
-- Slice 1/4: timeline pagination index on journal_entries.

CREATE INDEX idx_journal_entries_company_entry_date_id
    ON public.journal_entries USING btree (company_id, entry_date DESC, id DESC);
