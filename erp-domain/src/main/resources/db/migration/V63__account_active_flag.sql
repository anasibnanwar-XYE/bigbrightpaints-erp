-- Add active flag to accounts table for soft-delete support
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;
CREATE INDEX IF NOT EXISTS idx_accounts_active ON accounts(active);
