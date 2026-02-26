-- V21: Add username column to users table for porter login support
-- Porters use username + password instead of email to log in

ALTER TABLE users ADD COLUMN username VARCHAR(100) NULL;

-- Unique index on username (only non-null values)
-- MySQL allows multiple NULL values in unique indexes by default
CREATE UNIQUE INDEX idx_users_username_unique ON users (username);
