-- Orchestrator command idempotency ledger (prevent double-dispatch/double-posting from retries).
-- Scope is (company_id, command_name, idempotency_key).

CREATE TABLE IF NOT EXISTS orchestrator_commands (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      BIGINT          NOT NULL,
    command_name    VARCHAR(64)     NOT NULL,
    idempotency_key VARCHAR(255)    NOT NULL,
    request_hash    CHAR(64)        NOT NULL,
    trace_id        VARCHAR(128)    NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'IN_PROGRESS',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_error      TEXT,
    version         BIGINT          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'orchestrator_commands'
          AND constraint_name = 'fk_orchestrator_commands_company'
    ) THEN
        ALTER TABLE orchestrator_commands
            ADD CONSTRAINT fk_orchestrator_commands_company
            FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;
    END IF;
END$$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_orchestrator_commands_scope
    ON orchestrator_commands (company_id, command_name, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_orchestrator_commands_status
    ON orchestrator_commands (company_id, status, updated_at);

