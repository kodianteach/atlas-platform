-- V21: Add username column to users table for porter login support
-- Porters use username + password instead of email to log in

ALTER TABLE users ADD COLUMN username VARCHAR(100) NULL;

-- Unique index on username (only non-null values)
-- MySQL no soporta partial indexes, se usa un generated column como workaround
ALTER TABLE users ADD COLUMN username_unique VARCHAR(100)
    GENERATED ALWAYS AS (CASE WHEN username IS NOT NULL AND deleted_at IS NULL THEN username ELSE NULL END) STORED;

CREATE UNIQUE INDEX idx_users_username_unique ON users (username_unique);
 