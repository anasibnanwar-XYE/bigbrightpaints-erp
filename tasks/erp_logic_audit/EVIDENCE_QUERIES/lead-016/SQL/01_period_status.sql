-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Snapshot accounting period status and lock/close fields for LEAD-016.

SELECT
  id,
  year,
  month,
  start_date,
  end_date,
  status,
  locked_at,
  locked_by,
  lock_reason,
  reopened_at,
  reopened_by,
  reopen_reason,
  closed_at,
  closed_by
FROM accounting_periods
WHERE company_id = :company_id
ORDER BY year, month;
