-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Tie-out AP control balances vs supplier subledger totals.
-- Reference:
--   `ReconciliationService.reconcileApWithSupplierLedger` in accounting module.

WITH ap_accounts AS (
  SELECT a.id, a.code, a.balance
  FROM accounts a
  WHERE a.company_id = :company_id
    AND a.type = 'LIABILITY'
    AND a.code IS NOT NULL
    AND (
      UPPER(a.code) LIKE '%AP%'
      OR UPPER(a.code) LIKE '%PAYABLE%'
    )
),
ledger_total AS (
  SELECT COALESCE(SUM(sle.credit - sle.debit), 0) AS total
  FROM supplier_ledger_entries sle
  WHERE sle.company_id = :company_id
)
SELECT
  (SELECT COALESCE(SUM(balance), 0) FROM ap_accounts) AS gl_ap_balance,
  (SELECT total FROM ledger_total) AS supplier_ledger_total,
  (SELECT COALESCE(SUM(balance), 0) FROM ap_accounts) - (SELECT total FROM ledger_total) AS ap_variance;
