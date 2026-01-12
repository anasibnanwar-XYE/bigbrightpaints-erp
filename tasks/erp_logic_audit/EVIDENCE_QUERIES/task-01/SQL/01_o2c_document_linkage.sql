-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   O2C document linkage gaps: invoices and dispatched slips missing required journal links.

-- Invoices: status != DRAFT but journal link missing
SELECT
  'invoice_missing_je' AS finding_type,
  i.id AS invoice_id,
  i.invoice_number,
  i.status,
  i.issue_date,
  i.journal_entry_id
FROM invoices i
WHERE i.company_id = :company_id
  AND i.status <> 'DRAFT'
  AND i.journal_entry_id IS NULL
ORDER BY i.issue_date DESC, i.id DESC;

-- Dispatch slips: DISPATCHED but missing invoice/AR/COGS links
SELECT
  'dispatch_slip_missing_links' AS finding_type,
  ps.id AS slip_id,
  ps.slip_number,
  ps.status,
  ps.dispatched_at,
  ps.invoice_id,
  ps.journal_entry_id,
  ps.cogs_journal_entry_id
FROM packaging_slips ps
WHERE ps.company_id = :company_id
  AND ps.status = 'DISPATCHED'
  AND (
    ps.invoice_id IS NULL
    OR ps.journal_entry_id IS NULL
    OR ps.cogs_journal_entry_id IS NULL
  )
ORDER BY ps.dispatched_at DESC NULLS LAST, ps.id DESC;
