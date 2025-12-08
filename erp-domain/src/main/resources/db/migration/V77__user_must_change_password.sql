-- Add must_change_password flag for forcing password change on first login
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Set existing users with temporary passwords to require change (optional - uncomment if needed)
-- UPDATE app_users SET must_change_password = true WHERE created_at > NOW() - INTERVAL '24 hours';
