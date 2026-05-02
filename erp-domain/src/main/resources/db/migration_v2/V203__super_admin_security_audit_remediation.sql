CREATE TABLE IF NOT EXISTS super_admin_security_remediations (
    id BIGSERIAL PRIMARY KEY,
    audit_event_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    reason VARCHAR(300),
    updated_by VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_audit_event_id BIGINT,
    CONSTRAINT uk_super_admin_security_remediations_audit_event UNIQUE (audit_event_id),
    CONSTRAINT fk_super_admin_security_remediations_audit_event
        FOREIGN KEY (audit_event_id) REFERENCES audit_logs(id) ON DELETE RESTRICT,
    CONSTRAINT chk_super_admin_security_remediations_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT chk_super_admin_security_remediations_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_super_admin_security_remediations_reason
        CHECK (reason IS NULL OR length(trim(reason)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_super_admin_security_remediations_status
    ON super_admin_security_remediations(status);

CREATE INDEX IF NOT EXISTS idx_super_admin_security_remediations_updated
    ON super_admin_security_remediations(updated_at DESC, id DESC);
