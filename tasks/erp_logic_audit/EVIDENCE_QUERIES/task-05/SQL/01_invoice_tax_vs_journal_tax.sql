-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Compare invoice tax_total to posted output tax journal lines.

WITH tax_accounts AS (
  SELECT gst_output_tax_account_id AS output_tax_account_id
  FROM companies
  WHERE id = :company_id
),
invoice_tax AS (
  SELECT i.id,
         i.invoice_number,
         i.tax_total,
         i.journal_entry_id,
         i.issue_date
  FROM invoices i
  WHERE i.company_id = :company_id
),
journal_tax AS (
  SELECT je.id AS journal_entry_id,
         SUM(COALESCE(jl.credit, 0) - COALESCE(jl.debit, 0)) AS output_tax
  FROM journal_entries je
  JOIN journal_lines jl ON jl.journal_entry_id = je.id
  JOIN tax_accounts ta ON ta.output_tax_account_id = jl.account_id
  WHERE je.company_id = :company_id
  GROUP BY je.id
)
SELECT
  it.id AS invoice_id,
  it.invoice_number,
  it.issue_date,
  it.tax_total,
  COALESCE(jt.output_tax, 0) AS journal_output_tax,
  (COALESCE(jt.output_tax, 0) - COALESCE(it.tax_total, 0)) AS delta
FROM invoice_tax it
LEFT JOIN journal_tax jt ON jt.journal_entry_id = it.journal_entry_id
WHERE it.journal_entry_id IS NOT NULL
  AND ABS(COALESCE(jt.output_tax, 0) - COALESCE(it.tax_total, 0)) > 0.01
ORDER BY it.issue_date DESC, it.id DESC;
