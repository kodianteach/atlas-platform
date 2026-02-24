-- ============================================================================
-- V19: Tabla visitor_authorizations + permisos de autorizaciones
-- Historia #6: Generación de Autorizaciones con QR Firmado
-- ============================================================================

-- Tabla principal de autorizaciones de visitantes
CREATE TABLE visitor_authorizations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    unit_id INT NOT NULL,
    created_by_user_id INT NOT NULL,
    person_name VARCHAR(200) NOT NULL,
    person_document VARCHAR(50) NOT NULL,
    service_type VARCHAR(20) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    vehicle_plate VARCHAR(20),
    vehicle_type VARCHAR(20),
    vehicle_color VARCHAR(50),
    identity_document_key VARCHAR(500) NOT NULL,
    signed_qr TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    revoked_at TIMESTAMP NULL,
    revoked_by INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization(id),
    FOREIGN KEY (unit_id) REFERENCES unit(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    INDEX idx_va_org (organization_id),
    INDEX idx_va_unit (unit_id),
    INDEX idx_va_created_by (created_by_user_id),
    INDEX idx_va_status (status),
    INDEX idx_va_valid_range (valid_from, valid_to)
);

-- Permisos para el módulo de autorizaciones
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('AUTHORIZATIONS_CREATE', 'Crear Autorizaciones', 'Permite crear autorizaciones de ingreso', 'ACCESS_CONTROL', 'AUTHORIZATION', 'CREATE'),
('AUTHORIZATIONS_READ', 'Ver Autorizaciones', 'Permite ver autorizaciones de ingreso', 'ACCESS_CONTROL', 'AUTHORIZATION', 'READ'),
('AUTHORIZATIONS_REVOKE', 'Revocar Autorizaciones', 'Permite revocar autorizaciones de ingreso', 'ACCESS_CONTROL', 'AUTHORIZATION', 'REVOKE');

-- Asignar permisos a roles
-- ADMIN_ATLAS: CREATE, READ, REVOKE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS' AND p.code = 'AUTHORIZATIONS_CREATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS' AND p.code = 'AUTHORIZATIONS_READ';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS' AND p.code = 'AUTHORIZATIONS_REVOKE';

-- OWNER: CREATE, READ, REVOKE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'OWNER' AND p.code = 'AUTHORIZATIONS_CREATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'OWNER' AND p.code = 'AUTHORIZATIONS_READ';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'OWNER' AND p.code = 'AUTHORIZATIONS_REVOKE';

-- TENANT: CREATE, READ (sin REVOKE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'TENANT' AND p.code = 'AUTHORIZATIONS_CREATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'TENANT' AND p.code = 'AUTHORIZATIONS_READ';

-- FAMILY: CREATE, READ (sin REVOKE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'FAMILY' AND p.code = 'AUTHORIZATIONS_CREATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'FAMILY' AND p.code = 'AUTHORIZATIONS_READ';
