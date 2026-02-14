-- =============================================
-- Flyway Migration V10
-- Atlas Platform - User Identification & Units Enhancement
-- - Adds document type master table
-- - Adds identification fields to users
-- - Adds vehicles_enabled to units
-- - Enhances invitations for tracking
-- =============================================

-- =========================
-- DOCUMENT_TYPES (Master table for identification document types)
-- =========================
CREATE TABLE IF NOT EXISTS document_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE COMMENT 'CC, NIT, CE, TI, PA, PEP',
    name VARCHAR(100) NOT NULL COMMENT 'Human readable name',
    description VARCHAR(255) NULL,
    validation_regex VARCHAR(255) NULL COMMENT 'Regex pattern for format validation',
    min_length INT NULL,
    max_length INT NULL,
    applies_to ENUM('PERSON', 'COMPANY', 'BOTH') NOT NULL DEFAULT 'PERSON',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_document_types_code ON document_types(code);
CREATE INDEX idx_document_types_is_active ON document_types(is_active);

-- =========================
-- SEED: Colombian Document Types
-- =========================
INSERT INTO document_types (code, name, description, validation_regex, min_length, max_length, applies_to, sort_order) VALUES
('CC', 'Cédula de Ciudadanía', 'Documento de identidad colombiano para mayores de edad', '^[0-9]{6,10}$', 6, 10, 'PERSON', 1),
('NIT', 'Número de Identificación Tributaria', 'Identificación tributaria para empresas y personas jurídicas', '^[0-9]{9,10}(-[0-9])?$', 9, 11, 'COMPANY', 2),
('CE', 'Cédula de Extranjería', 'Documento para extranjeros residentes en Colombia', '^[0-9]{6,7}$', 6, 7, 'PERSON', 3),
('TI', 'Tarjeta de Identidad', 'Documento de identidad para menores de edad colombianos', '^[0-9]{10,11}$', 10, 11, 'PERSON', 4),
('PA', 'Pasaporte', 'Documento de viaje internacional', '^[A-Z0-9]{5,20}$', 5, 20, 'PERSON', 5),
('PEP', 'Permiso Especial de Permanencia', 'Permiso para migrantes venezolanos', '^[0-9]{15}$', 15, 15, 'PERSON', 6);

-- =========================
-- ALTER USERS: Add identification fields
-- =========================
ALTER TABLE users
    ADD COLUMN document_type VARCHAR(10) NULL AFTER phone,
    ADD COLUMN document_number VARCHAR(50) NULL AFTER document_type;

-- Unique constraint on document_type + document_number (only when both are not null)
-- Using a functional index approach for MySQL
CREATE UNIQUE INDEX idx_users_document_unique 
    ON users(document_type, document_number);

-- Foreign key to document_types (optional, for referential integrity)
ALTER TABLE users
    ADD CONSTRAINT fk_users_document_type 
    FOREIGN KEY (document_type) REFERENCES document_types(code) ON UPDATE CASCADE;

CREATE INDEX idx_users_document_type ON users(document_type);
CREATE INDEX idx_users_document_number ON users(document_number);

-- =========================
-- ALTER UNIT: Add vehicles_enabled flag
-- Note: max_vehicles already exists from V6
-- =========================
ALTER TABLE unit
    ADD COLUMN vehicles_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER max_vehicles;

-- Update existing units: if max_vehicles > 0, set vehicles_enabled = TRUE
UPDATE unit SET vehicles_enabled = TRUE WHERE max_vehicles > 0;

CREATE INDEX idx_unit_vehicles_enabled ON unit(vehicles_enabled);

-- =========================
-- ALTER INVITATIONS: Add tracking fields for resend and status
-- =========================
ALTER TABLE invitations
    ADD COLUMN invitation_sent_at TIMESTAMP NULL AFTER expires_at,
    ADD COLUMN invitation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER invitation_sent_at,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER invitation_status,
    ADD COLUMN last_retry_at TIMESTAMP NULL AFTER retry_count,
    ADD COLUMN metadata JSON NULL COMMENT 'Additional metadata for the invitation' AFTER last_retry_at;

-- Add OWNER_INVITATION to the type enum
ALTER TABLE invitations
    MODIFY COLUMN type ENUM('ORG_MEMBER','UNIT_OWNER','UNIT_TENANT','UNIT_FAMILY','OWNER_INVITATION') NOT NULL;

CREATE INDEX idx_invitations_invitation_status ON invitations(invitation_status);
CREATE INDEX idx_invitations_retry_count ON invitations(retry_count);

-- =========================
-- INVITATION AUDIT LOG (for tracking all invitation actions)
-- =========================
CREATE TABLE IF NOT EXISTS invitation_audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invitation_id INT NOT NULL,
    action ENUM('CREATED', 'SENT', 'RESENT', 'ACCEPTED', 'EXPIRED', 'CANCELLED', 'FAILED') NOT NULL,
    performed_by INT NULL COMMENT 'User who performed the action',
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    metadata JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invitation_audit_invitation FOREIGN KEY (invitation_id) REFERENCES invitations(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_audit_performed_by FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invitation_audit_invitation_id ON invitation_audit_log(invitation_id);
CREATE INDEX idx_invitation_audit_action ON invitation_audit_log(action);
CREATE INDEX idx_invitation_audit_created_at ON invitation_audit_log(created_at);

-- =========================
-- PERMISSIONS: Unit Distribution and Bulk Upload
-- =========================
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('UNITS_DISTRIBUTE', 'Crear Unidades por Distribución', 'Permite crear múltiples unidades por rango', 'ATLAS_CORE', 'units', 'DISTRIBUTE'),
('UNITS_BULK_UPLOAD', 'Carga Masiva de Unidades', 'Permite cargar unidades desde Excel/CSV', 'ATLAS_CORE', 'units', 'BULK_UPLOAD'),
('INVITATIONS_BULK_RESEND', 'Reenvío Masivo de Invitaciones', 'Permite reenviar múltiples invitaciones', 'ATLAS_CORE', 'invitations', 'BULK_RESEND');

-- Assign new permissions to ADMIN_ATLAS
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS'
AND p.code IN ('UNITS_DISTRIBUTE', 'UNITS_BULK_UPLOAD', 'INVITATIONS_BULK_RESEND');

-- Assign new permissions to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'SUPER_ADMIN'
AND p.code IN ('UNITS_DISTRIBUTE', 'UNITS_BULK_UPLOAD', 'INVITATIONS_BULK_RESEND');

-- =========================
-- CONFIGURATION: Invitation settings
-- =========================
CREATE TABLE IF NOT EXISTS system_configuration (
    id INT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255) NULL,
    config_type ENUM('STRING', 'INTEGER', 'BOOLEAN', 'JSON') NOT NULL DEFAULT 'STRING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_configuration (config_key, config_value, description, config_type) VALUES
('INVITATION_EXPIRATION_DAYS', '7', 'Default expiration days for invitations', 'INTEGER'),
('INVITATION_MAX_RETRY_COUNT', '3', 'Maximum number of invitation resend attempts', 'INTEGER'),
('INVITATION_RESEND_COOLDOWN_HOURS', '1', 'Minimum hours between resend attempts', 'INTEGER'),
('OWNER_INVITATION_EXPIRATION_DAYS', '7', 'Expiration days for owner invitations', 'INTEGER');

CREATE INDEX idx_system_configuration_key ON system_configuration(config_key);



