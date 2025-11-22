-- V51: Auth lockout controls

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

UPDATE app_users SET failed_login_attempts = 0 WHERE failed_login_attempts IS NULL;
