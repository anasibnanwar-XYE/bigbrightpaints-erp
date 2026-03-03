CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    category VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    github_issue_number BIGINT,
    github_issue_url VARCHAR(512),
    github_issue_state VARCHAR(32),
    github_synced_at TIMESTAMPTZ,
    github_last_error TEXT,
    github_last_sync_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    resolved_notification_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_support_tickets_category
        CHECK (category IN ('BUG', 'FEATURE_REQUEST', 'SUPPORT')),
    CONSTRAINT chk_support_tickets_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_support_tickets_company_created
    ON support_tickets(company_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_support_tickets_company_user_created
    ON support_tickets(company_id, user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_support_tickets_company_status_created
    ON support_tickets(company_id, status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_support_tickets_github_open_sync
    ON support_tickets(github_issue_number, status, created_at ASC)
    WHERE github_issue_number IS NOT NULL
      AND status IN ('OPEN', 'IN_PROGRESS');
