-- Backfill payroll run journal entry linkage
UPDATE payroll_runs
SET journal_entry_id = journal_entry_ref_id
WHERE journal_entry_id IS NULL
  AND journal_entry_ref_id IS NOT NULL;

UPDATE payroll_runs pr
SET journal_entry_ref_id = pr.journal_entry_id
WHERE pr.journal_entry_ref_id IS NULL
  AND pr.journal_entry_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM journal_entries je WHERE je.id = pr.journal_entry_id);
