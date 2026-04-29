ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS sla_policy_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS sla_support_tier VARCHAR(32),
    ADD COLUMN IF NOT EXISTS first_response_due_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS resolution_due_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sla_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS first_responded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS breached_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS converted_to_incident_at TIMESTAMPTZ;

UPDATE support_tickets
SET sla_status = 'NOT_APPLICABLE'
WHERE category = 'FEATURE_REQUEST'
  AND sla_status = 'PENDING';

UPDATE support_tickets t
SET sla_support_tier = COALESCE(NULLIF(UPPER(TRIM(c.commercial_support_tier)), ''), 'STANDARD'),
    sla_policy_id =
        COALESCE(NULLIF(UPPER(TRIM(c.commercial_support_tier)), ''), 'STANDARD')
            || '-' || COALESCE(t.priority, 'NORMAL'),
    first_response_due_at =
        t.created_at
            + make_interval(
                hours => GREATEST(
                    1,
                    CASE COALESCE(NULLIF(UPPER(TRIM(c.commercial_support_tier)), ''), 'STANDARD')
                        WHEN 'DEDICATED' THEN 2
                        WHEN 'ENTERPRISE' THEN 2
                        WHEN 'PRIORITY' THEN 4
                        ELSE 8
                    END
                    - CASE COALESCE(t.priority, 'NORMAL')
                        WHEN 'URGENT' THEN 3
                        WHEN 'HIGH' THEN 2
                        WHEN 'NORMAL' THEN 1
                        ELSE 0
                    END)),
    resolution_due_at =
        t.created_at
            + make_interval(
                hours => GREATEST(
                    4,
                    CASE COALESCE(NULLIF(UPPER(TRIM(c.commercial_support_tier)), ''), 'STANDARD')
                        WHEN 'DEDICATED' THEN 24
                        WHEN 'ENTERPRISE' THEN 24
                        WHEN 'PRIORITY' THEN 48
                        ELSE 72
                    END
                    - (
                        CASE COALESCE(t.priority, 'NORMAL')
                            WHEN 'URGENT' THEN 3
                            WHEN 'HIGH' THEN 2
                            WHEN 'NORMAL' THEN 1
                            ELSE 0
                        END
                    ) * 4)),
    sla_status = CASE WHEN t.status IN ('RESOLVED', 'CLOSED') THEN 'RESOLVED' ELSE 'PENDING' END
FROM companies c
WHERE t.company_id = c.id
  AND t.category <> 'FEATURE_REQUEST'
  AND (
      t.sla_policy_id IS NULL
      OR t.sla_support_tier IS NULL
      OR t.first_response_due_at IS NULL
      OR t.resolution_due_at IS NULL
  );

ALTER TABLE support_tickets
    DROP CONSTRAINT IF EXISTS chk_support_tickets_sla_status;

ALTER TABLE support_tickets
    ADD CONSTRAINT chk_support_tickets_sla_status
        CHECK (sla_status IN ('NOT_APPLICABLE', 'PENDING', 'RESPONDED', 'BREACHED', 'RESOLVED'));

CREATE INDEX IF NOT EXISTS idx_support_tickets_sla_status_due
    ON support_tickets(sla_status, resolution_due_at ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_support_tickets_category_status
    ON support_tickets(category, status, id ASC);

CREATE TABLE IF NOT EXISTS support_ticket_timeline_entries (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ticket_id BIGINT NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    from_category VARCHAR(32),
    to_category VARCHAR(32),
    note VARCHAR(512),
    audit_event_id BIGINT REFERENCES audit_logs(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_timeline_ticket_created
    ON support_ticket_timeline_entries(ticket_id, created_at ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_support_ticket_timeline_company_created
    ON support_ticket_timeline_entries(company_id, created_at DESC, id DESC);
