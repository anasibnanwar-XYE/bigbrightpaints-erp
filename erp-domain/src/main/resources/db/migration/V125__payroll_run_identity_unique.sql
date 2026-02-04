-- Enforce single payroll run per company/run_type/period window
CREATE UNIQUE INDEX IF NOT EXISTS ux_payroll_runs_company_period
    ON payroll_runs(company_id, run_type, period_start, period_end);

-- Speed idempotency lookups for payroll run creation
CREATE INDEX IF NOT EXISTS idx_payroll_runs_company_idempotency_key
    ON payroll_runs(company_id, idempotency_key);
