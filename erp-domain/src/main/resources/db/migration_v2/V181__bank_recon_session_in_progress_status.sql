ALTER TABLE bank_reconciliation_sessions
    ALTER COLUMN status SET DEFAULT 'IN_PROGRESS';

ALTER TABLE bank_reconciliation_sessions
    DROP CONSTRAINT IF EXISTS chk_bank_recon_session_status;

ALTER TABLE bank_reconciliation_sessions
    ADD CONSTRAINT chk_bank_recon_session_status
        CHECK (status IN ('IN_PROGRESS', 'DRAFT', 'COMPLETED'));
