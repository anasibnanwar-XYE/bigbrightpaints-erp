-- Add company scope to orchestrator outbox events (tenant isolation + traceability).
ALTER TABLE orchestrator_outbox
    ADD COLUMN IF NOT EXISTS company_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'orchestrator_outbox'
          AND constraint_name = 'fk_orchestrator_outbox_company'
    ) THEN
        ALTER TABLE orchestrator_outbox
            ADD CONSTRAINT fk_orchestrator_outbox_company
            FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_orchestrator_outbox_company_status
    ON orchestrator_outbox (company_id, status, dead_letter, next_attempt_at);

