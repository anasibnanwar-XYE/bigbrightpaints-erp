-- Database-backed token blacklist for distributed deployments
CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_token_revocations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_token_id ON blacklisted_tokens(token_id);
CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_user_id ON blacklisted_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_expires_at ON blacklisted_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_token_revocations_user_id ON user_token_revocations(user_id);
