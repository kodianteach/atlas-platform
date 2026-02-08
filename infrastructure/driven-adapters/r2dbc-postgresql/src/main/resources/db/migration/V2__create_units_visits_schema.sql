-- =============================================
-- Flyway Migration V2
-- Atlas Platform - Sistema de Unidades, Invitaciones y Visitas
-- =============================================

-- =========================
-- USER_UNITS (Vinculación usuarios a unidades)
-- =========================
CREATE TABLE IF NOT EXISTS user_units (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    unit_id INT NOT NULL,
    role_id INT NOT NULL COMMENT 'Rol dentro de la unidad',
    ownership_type ENUM('OWNER','TENANT','FAMILY','GUEST') NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Si es propietario principal',
    move_in_date DATE NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    invited_by INT NULL COMMENT 'Usuario que invitó',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_user_units_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_units_unit FOREIGN KEY (unit_id) REFERENCES unit(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_units_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_user_units_invited_by FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_user_units_user_unit ON user_units(user_id, unit_id);
CREATE INDEX idx_user_units_user_id ON user_units(user_id);
CREATE INDEX idx_user_units_unit_id ON user_units(unit_id);
CREATE INDEX idx_user_units_role_id ON user_units(role_id);
CREATE INDEX idx_user_units_status ON user_units(status);

-- =========================
-- USER_UNIT_PERMISSIONS (Permisos adicionales por unidad)
-- =========================
CREATE TABLE IF NOT EXISTS user_unit_permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_unit_id INT NOT NULL,
    permission_id INT NOT NULL,
    granted_by INT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL COMMENT 'NULL = permanente',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_unit_perms_user_unit FOREIGN KEY (user_unit_id) REFERENCES user_units(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_unit_perms_permission FOREIGN KEY (permission_id) REFERENCES permissions(id),
    CONSTRAINT fk_user_unit_perms_granted_by FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_user_unit_perms_unique ON user_unit_permissions(user_unit_id, permission_id);
CREATE INDEX idx_user_unit_perms_user_unit_id ON user_unit_permissions(user_unit_id);
CREATE INDEX idx_user_unit_perms_permission_id ON user_unit_permissions(permission_id);

-- =========================
-- INVITATIONS (Invitaciones por token)
-- =========================
CREATE TABLE IF NOT EXISTS invitations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    unit_id INT NULL COMMENT 'NULL si invitación a nivel org',
    email VARCHAR(255) NOT NULL,
    invitation_token VARCHAR(100) NOT NULL,
    type ENUM('ORG_MEMBER','UNIT_OWNER','UNIT_TENANT','UNIT_FAMILY') NOT NULL,
    role_id INT NULL,
    initial_permissions JSON NULL COMMENT 'IDs de permisos adicionales iniciales',
    status ENUM('PENDING','ACCEPTED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    invited_by_user_id INT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_invitations_organization FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_unit FOREIGN KEY (unit_id) REFERENCES unit(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_invitations_token ON invitations(invitation_token);
CREATE INDEX idx_invitations_organization_id ON invitations(organization_id);
CREATE INDEX idx_invitations_unit_id ON invitations(unit_id);
CREATE INDEX idx_invitations_email ON invitations(email);
CREATE INDEX idx_invitations_status ON invitations(status);
CREATE INDEX idx_invitations_expires_at ON invitations(expires_at);

-- =========================
-- VISIT_REQUESTS (Solicitudes de ingreso de visitantes)
-- =========================
CREATE TABLE IF NOT EXISTS visit_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    unit_id INT NOT NULL,
    requested_by INT NOT NULL COMMENT 'Usuario que solicita',
    visitor_name VARCHAR(200) NOT NULL,
    visitor_document VARCHAR(50) NULL,
    visitor_phone VARCHAR(40) NULL,
    visitor_email VARCHAR(160) NULL,
    vehicle_plate VARCHAR(20) NULL,
    purpose VARCHAR(255) NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    recurrence_type ENUM('ONCE','DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'ONCE',
    recurrence_days JSON NULL COMMENT 'Días de recurrencia [1,3,5] = L,M,V',
    max_entries INT NULL COMMENT 'NULL = ilimitado',
    status ENUM('PENDING','APPROVED','REJECTED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_visit_requests_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_visit_requests_unit FOREIGN KEY (unit_id) REFERENCES unit(id),
    CONSTRAINT fk_visit_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_visit_requests_organization_id ON visit_requests(organization_id);
CREATE INDEX idx_visit_requests_unit_id ON visit_requests(unit_id);
CREATE INDEX idx_visit_requests_requested_by ON visit_requests(requested_by);
CREATE INDEX idx_visit_requests_status ON visit_requests(status);
CREATE INDEX idx_visit_requests_valid_from ON visit_requests(valid_from);
CREATE INDEX idx_visit_requests_valid_until ON visit_requests(valid_until);

-- =========================
-- VISIT_APPROVALS (Aprobaciones/rechazos de visitas)
-- =========================
CREATE TABLE IF NOT EXISTS visit_approvals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    visit_request_id INT NOT NULL,
    approved_by INT NOT NULL,
    action ENUM('APPROVED','REJECTED') NOT NULL,
    reason TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visit_approvals_request FOREIGN KEY (visit_request_id) REFERENCES visit_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_approvals_approved_by FOREIGN KEY (approved_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_visit_approvals_visit_request_id ON visit_approvals(visit_request_id);
CREATE INDEX idx_visit_approvals_approved_by ON visit_approvals(approved_by);

-- =========================
-- ACCESS_CODES (QR/códigos de acceso)
-- =========================
CREATE TABLE IF NOT EXISTS access_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    visit_request_id INT NOT NULL,
    code_hash VARCHAR(255) NOT NULL COMMENT 'Hash del código/QR',
    code_type ENUM('QR','NUMERIC','ALPHANUMERIC') NOT NULL DEFAULT 'QR',
    status ENUM('ACTIVE','USED','EXPIRED','REVOKED') NOT NULL DEFAULT 'ACTIVE',
    entries_used INT NOT NULL DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_access_codes_visit_request FOREIGN KEY (visit_request_id) REFERENCES visit_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_access_codes_hash ON access_codes(code_hash);
CREATE INDEX idx_access_codes_visit_request_id ON access_codes(visit_request_id);
CREATE INDEX idx_access_codes_status ON access_codes(status);
CREATE INDEX idx_access_codes_valid_until ON access_codes(valid_until);

-- =========================
-- ACCESS_SCAN_LOG (Historial de escaneos en portería)
-- =========================
CREATE TABLE IF NOT EXISTS access_scan_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    access_code_id INT NOT NULL,
    scanned_by INT NULL COMMENT 'Usuario portero que escaneó',
    scan_result ENUM('VALID','INVALID','EXPIRED','ALREADY_USED','REVOKED') NOT NULL,
    scan_location VARCHAR(100) NULL COMMENT 'Portería/punto de acceso',
    device_info VARCHAR(255) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_access_scan_log_code FOREIGN KEY (access_code_id) REFERENCES access_codes(id),
    CONSTRAINT fk_access_scan_log_scanned_by FOREIGN KEY (scanned_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_access_scan_log_access_code_id ON access_scan_log(access_code_id);
CREATE INDEX idx_access_scan_log_scanned_by ON access_scan_log(scanned_by);
CREATE INDEX idx_access_scan_log_scan_result ON access_scan_log(scan_result);
CREATE INDEX idx_access_scan_log_created_at ON access_scan_log(created_at);
