ALTER TABLE orchestrator_audit
    ADD COLUMN IF NOT EXISTS company_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orchestrator_audit_company') THEN
        ALTER TABLE orchestrator_audit
            ADD CONSTRAINT fk_orchestrator_audit_company
            FOREIGN KEY (company_id)
            REFERENCES companies(id);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_orchestrator_audit_company_trace
    ON orchestrator_audit(company_id, trace_id, timestamp);
