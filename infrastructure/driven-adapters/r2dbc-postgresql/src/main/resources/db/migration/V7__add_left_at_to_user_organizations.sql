-- =========================
-- Add left_at column to user_organizations
-- =========================
ALTER TABLE user_organizations 
ADD COLUMN left_at TIMESTAMP NULL COMMENT 'Timestamp when user left the organization';
