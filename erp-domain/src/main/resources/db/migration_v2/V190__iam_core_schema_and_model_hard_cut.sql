CREATE TABLE IF NOT EXISTS iam_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    public_id UUID NOT NULL,
    account_type VARCHAR(24) NOT NULL,
    auth_scope_code VARCHAR(64) NOT NULL,
    company_id BIGINT REFERENCES companies(id) ON DELETE RESTRICT,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    locked_until TIMESTAMPTZ,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_iam_accounts_user_id UNIQUE (user_id),
    CONSTRAINT uq_iam_accounts_public_id UNIQUE (public_id),
    CONSTRAINT uq_iam_accounts_email_scope UNIQUE (email, auth_scope_code),
    CONSTRAINT chk_iam_accounts_type CHECK (account_type IN ('TENANT', 'PLATFORM')),
    CONSTRAINT chk_iam_accounts_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_iam_accounts_scope_present CHECK (btrim(auth_scope_code) <> ''),
    CONSTRAINT chk_iam_accounts_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT chk_iam_accounts_company_boundary CHECK (
        (account_type = 'PLATFORM' AND company_id IS NULL)
        OR (account_type = 'TENANT' AND company_id IS NOT NULL)
    ),
    CONSTRAINT chk_iam_accounts_failed_login_non_negative CHECK (failed_login_attempts >= 0)
);

CREATE TABLE IF NOT EXISTS iam_account_profiles (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES iam_accounts(id) ON DELETE CASCADE,
    display_name VARCHAR(255) NOT NULL,
    preferred_name VARCHAR(255),
    profile_picture_url VARCHAR(2048),
    job_title VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_iam_account_profiles_account_id UNIQUE (account_id)
);

CREATE TABLE IF NOT EXISTS iam_account_contacts (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES iam_accounts(id) ON DELETE CASCADE,
    primary_email VARCHAR(255) NOT NULL,
    secondary_email VARCHAR(255),
    phone_secondary VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_iam_account_contacts_account_id UNIQUE (account_id),
    CONSTRAINT chk_iam_account_contacts_primary_email_normalized CHECK (primary_email = lower(btrim(primary_email))),
    CONSTRAINT chk_iam_account_contacts_secondary_email_normalized CHECK (
        secondary_email IS NULL OR secondary_email = lower(btrim(secondary_email))
    )
);

CREATE TABLE IF NOT EXISTS iam_credentials (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES iam_accounts(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMPTZ,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_iam_credentials_account_id UNIQUE (account_id)
);

CREATE TABLE IF NOT EXISTS iam_mfa_factors (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES iam_accounts(id) ON DELETE CASCADE,
    factor_type VARCHAR(32) NOT NULL,
    encrypted_secret TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at TIMESTAMPTZ,
    disabled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_iam_mfa_factors_account_type UNIQUE (account_id, factor_type),
    CONSTRAINT chk_iam_mfa_factors_type CHECK (factor_type = 'TOTP'),
    CONSTRAINT chk_iam_mfa_factors_status CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED')),
    CONSTRAINT chk_iam_mfa_factors_secret_not_blank CHECK (btrim(encrypted_secret) <> '')
);

CREATE TABLE IF NOT EXISTS iam_devices (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES iam_accounts(id) ON DELETE CASCADE,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    device_label VARCHAR(255),
    user_agent_hash VARCHAR(128),
    ip_address_hash VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_iam_devices_public_id UNIQUE (public_id)
);

CREATE TABLE IF NOT EXISTS iam_sessions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES iam_accounts(id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES iam_devices(id) ON DELETE SET NULL,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    refresh_token_digest VARCHAR(64) NOT NULL,
    auth_scope_code VARCHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_iam_sessions_public_id UNIQUE (public_id),
    CONSTRAINT uq_iam_sessions_refresh_token_digest UNIQUE (refresh_token_digest),
    CONSTRAINT chk_iam_sessions_digest_length CHECK (length(refresh_token_digest) = 64),
    CONSTRAINT chk_iam_sessions_scope_present CHECK (btrim(auth_scope_code) <> '')
);

CREATE TABLE IF NOT EXISTS iam_security_events (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT REFERENCES iam_accounts(id) ON DELETE SET NULL,
    actor_account_id BIGINT REFERENCES iam_accounts(id) ON DELETE SET NULL,
    company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL,
    auth_scope_code VARCHAR(64),
    event_type VARCHAR(96) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_iam_security_events_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT chk_iam_security_events_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);

INSERT INTO iam_accounts (
    user_id,
    public_id,
    account_type,
    auth_scope_code,
    company_id,
    email,
    status,
    locked_until,
    failed_login_attempts,
    must_change_password,
    created_at
)
SELECT
    u.id,
    u.public_id,
    CASE WHEN u.company_id IS NULL THEN 'PLATFORM' ELSE 'TENANT' END,
    UPPER(TRIM(u.auth_scope_code)),
    u.company_id,
    LOWER(TRIM(u.email)),
    CASE WHEN u.enabled THEN 'ACTIVE' ELSE 'DISABLED' END,
    u.locked_until,
    u.failed_login_attempts,
    u.must_change_password,
    u.created_at
FROM app_users u
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO iam_account_profiles (account_id, display_name, preferred_name, profile_picture_url, job_title)
SELECT ia.id, u.display_name, u.preferred_name, u.profile_picture_url, u.job_title
FROM iam_accounts ia
JOIN app_users u ON u.id = ia.user_id
ON CONFLICT (account_id) DO NOTHING;

INSERT INTO iam_account_contacts (account_id, primary_email, secondary_email, phone_secondary)
SELECT ia.id, LOWER(TRIM(u.email)), NULLIF(LOWER(TRIM(u.secondary_email)), ''), u.phone_secondary
FROM iam_accounts ia
JOIN app_users u ON u.id = ia.user_id
ON CONFLICT (account_id) DO NOTHING;

INSERT INTO iam_credentials (account_id, password_hash, must_change_password, created_at)
SELECT ia.id, u.password_hash, u.must_change_password, u.created_at
FROM iam_accounts ia
JOIN app_users u ON u.id = ia.user_id
ON CONFLICT (account_id) DO NOTHING;

INSERT INTO iam_mfa_factors (account_id, factor_type, encrypted_secret, status, activated_at)
SELECT ia.id, 'TOTP', u.mfa_secret, CASE WHEN u.mfa_enabled THEN 'ACTIVE' ELSE 'PENDING' END, CASE WHEN u.mfa_enabled THEN now() ELSE NULL END
FROM iam_accounts ia
JOIN app_users u ON u.id = ia.user_id
WHERE u.mfa_secret IS NOT NULL AND btrim(u.mfa_secret) <> ''
ON CONFLICT (account_id, factor_type) DO NOTHING;

INSERT INTO mfa_recovery_codes (user_id, code_hash, created_at)
SELECT u.id, btrim(code_hash), now()
FROM app_users u
CROSS JOIN LATERAL regexp_split_to_table(COALESCE(u.mfa_recovery_codes, ''), ',') AS code_hash
WHERE btrim(code_hash) <> ''
ON CONFLICT DO NOTHING;

ALTER TABLE refresh_tokens
    DROP COLUMN IF EXISTS token;

ALTER TABLE refresh_tokens
    ALTER COLUMN token_digest SET NOT NULL;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT chk_refresh_tokens_token_digest_length CHECK (length(token_digest) = 64);

ALTER TABLE password_reset_tokens
    DROP CONSTRAINT IF EXISTS password_reset_tokens_token_key;

ALTER TABLE password_reset_tokens
    DROP COLUMN IF EXISTS token;

ALTER TABLE password_reset_tokens
    ALTER COLUMN token_digest SET NOT NULL;

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT chk_password_reset_tokens_token_digest_length CHECK (length(token_digest) = 64);

ALTER TABLE app_users
    DROP COLUMN IF EXISTS mfa_recovery_codes;

CREATE INDEX IF NOT EXISTS idx_iam_accounts_company_id ON iam_accounts (company_id);
CREATE INDEX IF NOT EXISTS idx_iam_accounts_scope ON iam_accounts (auth_scope_code);
CREATE INDEX IF NOT EXISTS idx_iam_contacts_secondary_email ON iam_account_contacts (secondary_email) WHERE secondary_email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_iam_mfa_factors_account_status ON iam_mfa_factors (account_id, status);
CREATE INDEX IF NOT EXISTS idx_iam_sessions_account_active ON iam_sessions (account_id, revoked_at, expires_at);
CREATE INDEX IF NOT EXISTS idx_iam_security_events_account_time ON iam_security_events (account_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_iam_security_events_scope_time ON iam_security_events (auth_scope_code, occurred_at DESC);

COMMENT ON TABLE iam_accounts IS 'Canonical IAM account identity/status/scope model; app_users remains the legacy runtime adapter until downstream service slices switch repositories.';
COMMENT ON TABLE iam_account_profiles IS 'Identity-owned profile fields only: display/preferred name, profile picture URL, and optional job title.';
COMMENT ON TABLE iam_account_contacts IS 'Identity-owned contact fields only: primary email plus self-owned secondary email and phone.';
COMMENT ON TABLE iam_credentials IS 'Canonical credential verifier metadata; password hashes stay verifier-only and are never API output.';
COMMENT ON TABLE iam_mfa_factors IS 'Canonical MFA factor table for encrypted authenticator TOTP secrets.';
COMMENT ON TABLE mfa_recovery_codes IS 'Canonical verifier-only MFA recovery-code store; app_users.mfa_recovery_codes is removed by V190.';
COMMENT ON TABLE iam_sessions IS 'Canonical first-class session table with digest-only refresh-token verifier storage.';
COMMENT ON TABLE iam_devices IS 'Canonical sanitized device metadata for IAM sessions.';
COMMENT ON TABLE iam_security_events IS 'Canonical redacted IAM security-event stream; metadata must never contain raw secrets, tokens, hashes, passwords, MFA secrets, recovery codes, or unnecessary PII.';
