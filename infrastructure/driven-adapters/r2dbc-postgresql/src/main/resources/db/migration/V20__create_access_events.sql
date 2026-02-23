-- ============================================================================
-- V20: Tabla access_events + permisos de validación/registro de acceso
-- Historia #7: Validación en Portería — Scanner QR Online/Offline
-- ============================================================================

-- Tabla de eventos de acceso (independiente de access_scan_log)
-- Referencia visitor_authorizations (HU #6), no access_codes (HU legacy)
CREATE TABLE access_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    authorization_id INT NULL,
    porter_user_id INT NOT NULL,
    device_id VARCHAR(100) NULL,
    action VARCHAR(10) NOT NULL COMMENT 'ENTRY o EXIT',
    scan_result VARCHAR(20) NOT NULL COMMENT 'VALID, INVALID, EXPIRED, REVOKED',
    person_name VARCHAR(200) NULL,
    person_document VARCHAR(50) NULL,
    vehicle_plate VARCHAR(20) NULL,
    vehicle_match BOOLEAN NULL,
    offline_validated BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL,
    scanned_at TIMESTAMP NOT NULL,
    synced_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization(id),
    FOREIGN KEY (authorization_id) REFERENCES visitor_authorizations(id),
    FOREIGN KEY (porter_user_id) REFERENCES users(id),
    INDEX idx_ae_org (organization_id),
    INDEX idx_ae_auth (authorization_id),
    INDEX idx_ae_porter (porter_user_id),
    INDEX idx_ae_action (action),
    INDEX idx_ae_scanned_at (scanned_at),
    INDEX idx_ae_person_doc (person_document)
);

-- Permisos para validación y registro de acceso en portería
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('AUTHORIZATIONS_VALIDATE', 'Validar Autorizaciones', 'Permite validar autorizaciones de ingreso mediante QR o documento', 'ACCESS_CONTROL', 'AUTHORIZATION', 'VALIDATE'),
('ACCESS_LOG_CREATE', 'Registrar Eventos de Acceso', 'Permite registrar eventos de entrada/salida', 'ACCESS_CONTROL', 'ACCESS_LOG', 'CREATE');

-- Asignar permisos a roles de portería
-- PORTERO_GENERAL: VALIDATE, LOG_CREATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'PORTERO_GENERAL' AND p.code = 'AUTHORIZATIONS_VALIDATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'PORTERO_GENERAL' AND p.code = 'ACCESS_LOG_CREATE';

-- PORTERO_DELIVERY: VALIDATE, LOG_CREATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'PORTERO_DELIVERY' AND p.code = 'AUTHORIZATIONS_VALIDATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'PORTERO_DELIVERY' AND p.code = 'ACCESS_LOG_CREATE';

-- SECURITY: VALIDATE, LOG_CREATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'SECURITY' AND p.code = 'AUTHORIZATIONS_VALIDATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'SECURITY' AND p.code = 'ACCESS_LOG_CREATE';

-- ADMIN_ATLAS: VALIDATE, LOG_CREATE (para acceso administrativo)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS' AND p.code = 'AUTHORIZATIONS_VALIDATE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS' AND p.code = 'ACCESS_LOG_CREATE';
