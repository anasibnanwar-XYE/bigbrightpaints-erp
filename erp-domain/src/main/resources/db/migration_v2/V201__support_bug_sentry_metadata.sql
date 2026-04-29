ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS bug_reproduction_steps TEXT,
    ADD COLUMN IF NOT EXISTS bug_environment VARCHAR(64),
    ADD COLUMN IF NOT EXISTS bug_release VARCHAR(128),
    ADD COLUMN IF NOT EXISTS bug_trace_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS bug_metadata_json TEXT,
    ADD COLUMN IF NOT EXISTS sentry_issue_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS sentry_issue_url VARCHAR(512),
    ADD COLUMN IF NOT EXISTS sentry_issue_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS sentry_linked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sentry_synced_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sentry_last_sync_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sentry_last_error TEXT;

CREATE INDEX IF NOT EXISTS idx_support_tickets_sentry_issue
    ON support_tickets(sentry_issue_id)
    WHERE sentry_issue_id IS NOT NULL;
