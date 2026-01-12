-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Snapshot GST return amounts from journal lines for the current month.

WITH period AS (
  SELECT
    date_trunc('month', CURRENT_DATE)::date AS start_date,
    (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month - 1 day')::date AS end_date
),
tax_accounts AS (
  SELECT gst_input_tax_account_id AS input_tax_account_id,
         gst_output_tax_account_id AS output_tax_account_id
  FROM companies
  WHERE id = :company_id
),
output_tax AS (
  SELECT SUM(COALESCE(jl.credit, 0) - COALESCE(jl.debit, 0)) AS output_tax
  FROM journal_entries je
  JOIN journal_lines jl ON jl.journal_entry_id = je.id
  JOIN tax_accounts ta ON ta.output_tax_account_id = jl.account_id
  JOIN period p ON je.entry_date BETWEEN p.start_date AND p.end_date
  WHERE je.company_id = :company_id
),
input_tax AS (
  SELECT SUM(COALESCE(jl.debit, 0) - COALESCE(jl.credit, 0)) AS input_tax
  FROM journal_entries je
  JOIN journal_lines jl ON jl.journal_entry_id = je.id
  JOIN tax_accounts ta ON ta.input_tax_account_id = jl.account_id
  JOIN period p ON je.entry_date BETWEEN p.start_date AND p.end_date
  WHERE je.company_id = :company_id
)
SELECT
  p.start_date,
  p.end_date,
  COALESCE(o.output_tax, 0) AS output_tax,
  COALESCE(i.input_tax, 0) AS input_tax,
  COALESCE(o.output_tax, 0) - COALESCE(i.input_tax, 0) AS net_payable
FROM period p
CROSS JOIN output_tax o
CROSS JOIN input_tax i;
