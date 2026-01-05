-- Indexes to speed payroll run list endpoints
CREATE INDEX IF NOT EXISTS idx_payroll_runs_company_created_at
    ON payroll_runs(company_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payroll_runs_company_type_created_at
    ON payroll_runs(company_id, run_type, created_at DESC);
