ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS portal_user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS receivable_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_dealers_portal_user ON dealers(portal_user_id);
CREATE INDEX IF NOT EXISTS idx_dealers_receivable_account ON dealers(receivable_account_id);
