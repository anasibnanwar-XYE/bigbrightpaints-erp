ALTER TABLE super_admin_billing_subscriptions
    ADD COLUMN IF NOT EXISTS pending_commercial_action VARCHAR(32),
    ADD COLUMN IF NOT EXISTS pending_commercial_effective_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pending_commercial_reason VARCHAR(300),
    ADD COLUMN IF NOT EXISTS last_commercial_action VARCHAR(32),
    ADD COLUMN IF NOT EXISTS last_commercial_action_fingerprint VARCHAR(128),
    ADD COLUMN IF NOT EXISTS last_commercial_action_effective_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_commercial_action_audit_event_id BIGINT;

ALTER TABLE super_admin_billing_subscriptions
    DROP CONSTRAINT IF EXISTS chk_super_admin_billing_pending_commercial_action,
    ADD CONSTRAINT chk_super_admin_billing_pending_commercial_action
        CHECK (
            pending_commercial_action IS NULL
            OR pending_commercial_action IN ('CANCEL', 'ARCHIVE')
        ),
    DROP CONSTRAINT IF EXISTS chk_super_admin_billing_last_commercial_action,
    ADD CONSTRAINT chk_super_admin_billing_last_commercial_action
        CHECK (
            last_commercial_action IS NULL
            OR last_commercial_action IN (
                'GRACE',
                'SUSPEND_READ_ONLY',
                'SUSPEND_BLOCKED',
                'RESUME',
                'CANCEL',
                'ARCHIVE'
            )
        ),
    DROP CONSTRAINT IF EXISTS chk_super_admin_billing_pending_commercial_pair,
    ADD CONSTRAINT chk_super_admin_billing_pending_commercial_pair
        CHECK (
            (pending_commercial_action IS NULL AND pending_commercial_effective_at IS NULL)
            OR (pending_commercial_action IS NOT NULL AND pending_commercial_effective_at IS NOT NULL)
        );

CREATE INDEX IF NOT EXISTS idx_super_admin_billing_pending_commercial
    ON super_admin_billing_subscriptions(pending_commercial_effective_at, pending_commercial_action)
    WHERE pending_commercial_action IS NOT NULL;
