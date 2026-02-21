-- ============================================================================
-- V15: Roles de portero, permisos nuevos y tokens de enrolamiento
-- Historia #4: Creación de Portero (Gatekeeper)
--
-- Principio: Un portero ES un usuario (tabla users) con un rol específico
-- asignado via user_roles_multi. NO se crea tabla separada "porters".
-- El token de enrolamiento sigue el patrón de admin_activation_tokens (V5).
-- ============================================================================

-- =========================
-- ROLES DE PORTERO
-- Tabla: role (V1)
-- =========================
INSERT INTO role (name, code, description, module_code, is_system) VALUES
('Portero General', 'PORTERO_GENERAL', 'Portero general de propiedad horizontal', 'ACCESS_CONTROL', TRUE),
('Portero Delivery', 'PORTERO_DELIVERY', 'Portero dedicado a entregas y delivery', 'ACCESS_CONTROL', TRUE);

-- =========================
-- PERMISOS NUEVOS (solo los que no existen aún)
-- VEHICLES_VALIDATE ya existe en V6
-- ACCESS_CODES_VALIDATE ya existe en V3
-- ACCESS_LOG_READ ya existe en V3
-- =========================
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('PORTERS_READ', 'Ver Porteros', 'Permite ver lista de porteros de la organización', 'ACCESS_CONTROL', 'porters', 'READ'),
('PORTERS_CREATE', 'Crear Porteros', 'Permite crear porteros y generar URL de enrolamiento', 'ACCESS_CONTROL', 'porters', 'CREATE'),
('PORTERS_MANAGE', 'Gestionar Porteros', 'Permite gestionar porteros (regenerar URL, desactivar)', 'ACCESS_CONTROL', 'porters', 'MANAGE');

-- =========================
-- ASIGNAR PERMISOS A ROL PORTERO_GENERAL
-- Reutiliza permisos existentes: VISITS_READ, ACCESS_CODES_VALIDATE, ACCESS_LOG_READ, VEHICLES_VALIDATE
-- =========================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permissions p
WHERE r.code = 'PORTERO_GENERAL'
  AND p.code IN ('VISITS_READ', 'ACCESS_CODES_VALIDATE', 'ACCESS_LOG_READ', 'VEHICLES_VALIDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- =========================
-- ASIGNAR PERMISOS A ROL PORTERO_DELIVERY
-- Mismo set de permisos que PORTERO_GENERAL
-- =========================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permissions p
WHERE r.code = 'PORTERO_DELIVERY'
  AND p.code IN ('VISITS_READ', 'ACCESS_CODES_VALIDATE', 'ACCESS_LOG_READ', 'VEHICLES_VALIDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- =========================
-- ASIGNAR PERMISOS DE GESTIÓN DE PORTEROS A ADMIN_ATLAS
-- =========================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS'
  AND p.code IN ('PORTERS_READ', 'PORTERS_CREATE', 'PORTERS_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- =========================
-- PORTER ENROLLMENT TOKENS
-- Sigue el patrón de admin_activation_tokens (V5)
-- Sin initial_password_hash (porteros se enrolan por URL, no por contraseña)
-- =========================
CREATE TABLE IF NOT EXISTS porter_enrollment_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT 'Usuario portero pre-registrado en users',
    organization_id INT NOT NULL COMMENT 'Organizacion donde operara el portero',
    token_hash VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'CONSUMED', 'EXPIRED', 'REVOKED') NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_by INT NULL COMMENT 'Admin que genero el token',
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    activation_ip VARCHAR(45) NULL COMMENT 'IP desde donde se consumio el token',
    activation_user_agent TEXT NULL COMMENT 'User-Agent de consumo',
    metadata JSON NULL COMMENT 'Datos adicionales (display_name, porter_type)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_porter_enroll_token_hash (token_hash),
    INDEX idx_porter_enroll_user_id (user_id),
    INDEX idx_porter_enroll_org_id (organization_id),
    INDEX idx_porter_enroll_status (status),
    INDEX idx_porter_enroll_expires_at (expires_at),
    CONSTRAINT fk_porter_enroll_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_porter_enroll_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_porter_enroll_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================
-- PORTER ENROLLMENT AUDIT LOG
-- Sigue el patrón de preregistration_audit_log (V5)
-- =========================
CREATE TABLE IF NOT EXISTS porter_enrollment_audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token_id INT NOT NULL,
    action ENUM('CREATED', 'URL_GENERATED', 'URL_REGENERATED', 'CONSUMED', 'EXPIRED', 'REVOKED') NOT NULL,
    performed_by INT NULL COMMENT 'NULL si es accion del sistema',
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    details JSON NULL COMMENT 'Detalles adicionales de la accion',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_porter_audit_token_id (token_id),
    INDEX idx_porter_audit_action (action),
    CONSTRAINT fk_porter_audit_token FOREIGN KEY (token_id) REFERENCES porter_enrollment_tokens(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
