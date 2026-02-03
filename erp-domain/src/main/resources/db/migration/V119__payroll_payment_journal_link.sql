-- Track payroll payment journal separately from payroll posting journal (avoid double-expense + preserve audit trail).
ALTER TABLE payroll_runs
    ADD COLUMN IF NOT EXISTS payment_journal_entry_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'payroll_runs'
          AND constraint_name = 'fk_payroll_runs_payment_journal_entry'
    ) THEN
        ALTER TABLE payroll_runs
            ADD CONSTRAINT fk_payroll_runs_payment_journal_entry
            FOREIGN KEY (payment_journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_payroll_runs_payment_journal
    ON payroll_runs (company_id, payment_journal_entry_id);

