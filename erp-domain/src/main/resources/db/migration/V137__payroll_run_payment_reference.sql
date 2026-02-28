ALTER TABLE payroll_runs
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255);
