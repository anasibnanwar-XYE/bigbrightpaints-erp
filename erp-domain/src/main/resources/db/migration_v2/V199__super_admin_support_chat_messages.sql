ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL';

ALTER TABLE support_tickets
    DROP CONSTRAINT IF EXISTS chk_support_tickets_priority;

ALTER TABLE support_tickets
    ADD CONSTRAINT chk_support_tickets_priority
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));

CREATE INDEX IF NOT EXISTS idx_support_tickets_platform_queue
    ON support_tickets(status, priority, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS support_ticket_messages (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ticket_id BIGINT NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    author_user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    author_role VARCHAR(32) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    audit_event_id BIGINT REFERENCES audit_logs(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_support_ticket_messages_author_role
        CHECK (author_role IN ('TENANT', 'SUPER_ADMIN')),
    CONSTRAINT chk_support_ticket_messages_visibility
        CHECK (visibility IN ('CUSTOMER', 'INTERNAL'))
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_messages_ticket_visible_created
    ON support_ticket_messages(ticket_id, visibility, created_at ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_support_ticket_messages_company_created
    ON support_ticket_messages(company_id, created_at DESC, id DESC);
