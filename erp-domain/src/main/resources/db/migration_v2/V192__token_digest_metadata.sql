-- M5 digest metadata hardening: token tables must persist digest algorithm/version metadata.
-- Backfill existing digest-only rows and enforce metadata without adding any raw token/link fields.

ALTER TABLE password_reset_tokens
    ADD COLUMN IF NOT EXISTS digest_algorithm VARCHAR(32),
    ADD COLUMN IF NOT EXISTS digest_version INTEGER;

UPDATE password_reset_tokens
SET digest_algorithm = 'SHA-256'
WHERE digest_algorithm IS NULL;

UPDATE password_reset_tokens
SET digest_version = 1
WHERE digest_version IS NULL;

ALTER TABLE password_reset_tokens
    ALTER COLUMN digest_algorithm SET NOT NULL,
    ALTER COLUMN digest_version SET NOT NULL;

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT chk_password_reset_tokens_digest_algorithm
        CHECK (digest_algorithm = 'SHA-256'),
    ADD CONSTRAINT chk_password_reset_tokens_digest_version
        CHECK (digest_version = 1);

ALTER TABLE tenant_activation_tokens
    ADD COLUMN IF NOT EXISTS digest_algorithm VARCHAR(32),
    ADD COLUMN IF NOT EXISTS digest_version INTEGER;

UPDATE tenant_activation_tokens
SET digest_algorithm = 'SHA-256'
WHERE digest_algorithm IS NULL;

UPDATE tenant_activation_tokens
SET digest_version = 1
WHERE digest_version IS NULL;

ALTER TABLE tenant_activation_tokens
    ALTER COLUMN digest_algorithm SET NOT NULL,
    ALTER COLUMN digest_version SET NOT NULL;

ALTER TABLE tenant_activation_tokens
    ADD CONSTRAINT chk_tenant_activation_tokens_digest_algorithm
        CHECK (digest_algorithm = 'SHA-256'),
    ADD CONSTRAINT chk_tenant_activation_tokens_digest_version
        CHECK (digest_version = 1);
