ALTER TABLE app_users
    DROP COLUMN IF EXISTS reset_token,
    DROP COLUMN IF EXISTS reset_expiry;
