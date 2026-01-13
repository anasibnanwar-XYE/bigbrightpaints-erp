-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   List journals and lines for recent production logs (RM, SEMIFG, PACK, CAL).

WITH recent_logs AS (
  SELECT pl.production_code
  FROM production_logs pl
  WHERE pl.company_id = :company_id
  ORDER BY pl.id DESC
  LIMIT 5
)
SELECT
  rl.production_code,
  je.reference_number,
  jl.account_id,
  a.code AS account_code,
  a.name AS account_name,
  jl.debit,
  jl.credit
FROM recent_logs rl
JOIN journal_entries je
  ON je.company_id = :company_id
 AND (
   je.reference_number = rl.production_code || '-RM'
   OR je.reference_number = rl.production_code || '-SEMIFG'
   OR je.reference_number LIKE rl.production_code || '-PACK-%'
   OR je.reference_number = 'CAL-' || rl.production_code
 )
JOIN journal_lines jl ON jl.journal_entry_id = je.id
LEFT JOIN accounts a ON a.id = jl.account_id
ORDER BY rl.production_code, je.reference_number, jl.id;
