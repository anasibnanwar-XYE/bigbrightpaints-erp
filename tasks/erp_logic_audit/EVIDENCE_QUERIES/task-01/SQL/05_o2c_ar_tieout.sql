-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Tie-out AR control balances vs dealer subledger totals.
-- Reference:
--   `ReconciliationService.reconcileArWithDealerLedger` in accounting module.

WITH ar_accounts AS (
  SELECT a.id, a.code, a.balance
  FROM accounts a
  WHERE a.company_id = :company_id
    AND a.type = 'ASSET'
    AND a.code IS NOT NULL
    AND (
      UPPER(a.code) LIKE '%AR%'
      OR UPPER(a.code) LIKE '%RECEIVABLE%'
    )
),
ledger_total AS (
  SELECT COALESCE(SUM(dle.debit - dle.credit), 0) AS total
  FROM dealer_ledger_entries dle
  WHERE dle.company_id = :company_id
)
SELECT
  (SELECT COALESCE(SUM(balance), 0) FROM ar_accounts) AS gl_ar_balance,
  (SELECT total FROM ledger_total) AS dealer_ledger_total,
  (SELECT COALESCE(SUM(balance), 0) FROM ar_accounts) - (SELECT total FROM ledger_total) AS ar_variance;
