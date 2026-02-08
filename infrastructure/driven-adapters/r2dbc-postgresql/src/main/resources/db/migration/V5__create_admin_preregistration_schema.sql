-- =============================================
-- Flyway Migration V5
-- Atlas Platform - Pre-registro de Administradores
-- =============================================

-- =========================
-- ALTER USERS: Agregar columna status
-- =========================
ALTER TABLE users 
ADD COLUMN status ENUM('ACTIVE', 'PRE_REGISTERED', 'PENDING_ACTIVATION', 'ACTIVATED', 'SUSPENDED') 
NOT NULL DEFAULT 'ACTIVE' AFTER is_active;

CREATE INDEX idx_users_status ON users(status);

-- =========================
-- ADMIN ACTIVATION TOKENS
-- Tokens para activación de administradores pre-registrados
-- =========================
CREATE TABLE IF NOT EXISTS admin_activation_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    initial_password_hash VARCHAR(255) NOT NULL COMMENT 'Hash de la contraseña temporal enviada al usuario',
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    status ENUM('PENDING', 'CONSUMED', 'EXPIRED', 'REVOKED') NOT NULL DEFAULT 'PENDING',
    created_by INT NULL COMMENT 'ID del operador que creó el pre-registro',
    ip_address VARCHAR(45) NULL COMMENT 'IP desde donde se creó',
    user_agent TEXT NULL COMMENT 'User-Agent del cliente que creó el token',
    activation_ip VARCHAR(45) NULL COMMENT 'IP desde donde se activó',
    activation_user_agent TEXT NULL COMMENT 'User-Agent de activación',
    metadata JSON NULL COMMENT 'Metadata adicional (company_name, org_name sugeridos, etc.)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_activation_tokens_hash ON admin_activation_tokens(token_hash);
CREATE INDEX idx_activation_tokens_user_id ON admin_activation_tokens(user_id);
CREATE INDEX idx_activation_tokens_status ON admin_activation_tokens(status);
CREATE INDEX idx_activation_tokens_expires_at ON admin_activation_tokens(expires_at);

ALTER TABLE admin_activation_tokens 
ADD CONSTRAINT fk_activation_token_user 
FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE admin_activation_tokens 
ADD CONSTRAINT fk_activation_token_created_by 
FOREIGN KEY (created_by) REFERENCES users(id);

-- =========================
-- PRE-REGISTRATION AUDIT LOG
-- Historial de acciones sobre pre-registros
-- =========================
CREATE TABLE IF NOT EXISTS preregistration_audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token_id INT NOT NULL,
    action ENUM('CREATED', 'EMAIL_SENT', 'ACTIVATED', 'EXPIRED', 'REVOKED', 'RESENT') NOT NULL,
    performed_by INT NULL COMMENT 'NULL si es acción del sistema',
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    details JSON NULL COMMENT 'Detalles adicionales de la acción',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_preregistration_audit_token_id ON preregistration_audit_log(token_id);
CREATE INDEX idx_preregistration_audit_action ON preregistration_audit_log(action);

ALTER TABLE preregistration_audit_log 
ADD CONSTRAINT fk_preregistration_audit_token 
FOREIGN KEY (token_id) REFERENCES admin_activation_tokens(id);

-- =========================
-- CONFIGURACIÓN: Agregar permisos para pre-registro
-- =========================
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('ADMIN_PREREGISTER_CREATE', 'Crear pre-registro de admin', 'Permite crear pre-registros de administradores', NULL, 'admin_preregister', 'CREATE'),
('ADMIN_PREREGISTER_READ', 'Ver pre-registros de admin', 'Permite ver pre-registros de administradores', NULL, 'admin_preregister', 'READ'),
('ADMIN_PREREGISTER_REVOKE', 'Revocar pre-registro de admin', 'Permite revocar pre-registros de administradores', NULL, 'admin_preregister', 'DELETE'),
('ADMIN_PREREGISTER_RESEND', 'Reenviar pre-registro de admin', 'Permite reenviar correos de pre-registro', NULL, 'admin_preregister', 'UPDATE');

-- Asignar permisos al SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM role r, permissions p 
WHERE r.code = 'SUPER_ADMIN' 
AND p.code IN ('ADMIN_PREREGISTER_CREATE', 'ADMIN_PREREGISTER_READ', 'ADMIN_PREREGISTER_REVOKE', 'ADMIN_PREREGISTER_RESEND');
