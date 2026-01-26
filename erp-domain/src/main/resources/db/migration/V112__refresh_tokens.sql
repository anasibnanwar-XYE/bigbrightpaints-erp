CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_email VARCHAR(255) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_email ON refresh_tokens (user_email);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
