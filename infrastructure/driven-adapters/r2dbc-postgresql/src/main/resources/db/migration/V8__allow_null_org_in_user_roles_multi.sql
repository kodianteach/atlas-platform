-- =========================
-- Allow NULL organization_id in user_roles_multi
-- This supports pre-registered admins who have roles assigned
-- before completing onboarding (creating their organization)
-- =========================

-- Remove the foreign key constraint first
ALTER TABLE user_roles_multi 
DROP FOREIGN KEY fk_user_roles_multi_organization;

-- Drop the unique constraint that includes organization_id
ALTER TABLE user_roles_multi 
DROP INDEX uq_user_roles_multi;

-- Modify the column to allow NULL
ALTER TABLE user_roles_multi 
MODIFY COLUMN organization_id INT NULL;

-- Re-add the foreign key constraint with SET NULL option
ALTER TABLE user_roles_multi 
ADD CONSTRAINT fk_user_roles_multi_organization 
FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE SET NULL;

-- Re-add unique constraint (allowing multiple NULLs is default in MySQL for unique)
ALTER TABLE user_roles_multi 
ADD CONSTRAINT uq_user_roles_multi UNIQUE (user_id, organization_id, role_id);
