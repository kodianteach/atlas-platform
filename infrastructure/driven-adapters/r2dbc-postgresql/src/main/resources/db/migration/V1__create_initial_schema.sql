-- =============================================
-- Flyway Migration V1
-- Atlas Platform - Administración de Organizaciones Residenciales
-- Esquema inicial - MySQL
-- =============================================

-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    names VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    phone VARCHAR(40),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    last_organization_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_last_organization_id ON users(last_organization_id);

-- =========================
-- ROLES
-- =========================
CREATE TABLE IF NOT EXISTS role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    code VARCHAR(50) NULL UNIQUE,
    description TEXT NULL,
    module_code VARCHAR(50) NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_role_code ON role(code);
CREATE INDEX idx_role_module_code ON role(module_code);

CREATE TABLE IF NOT EXISTS user_role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================
-- COMPANY (Holding de organizaciones)
-- =========================
CREATE TABLE IF NOT EXISTS company (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(100) NULL UNIQUE,
    tax_id VARCHAR(40),
    industry VARCHAR(120),
    website VARCHAR(255),
    address TEXT,
    country VARCHAR(80),
    city VARCHAR(80),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_company_slug ON company(slug);
CREATE INDEX idx_company_status ON company(status);

-- =========================
-- ORGANIZATION (Ciudadela o Conjunto Residencial)
-- =========================
CREATE TABLE IF NOT EXISTS organization (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(100) NULL,
    type ENUM('CIUDADELA','CONJUNTO') NOT NULL COMMENT 'Tipo de organización residencial',
    uses_zones BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Si usa zonas o no',
    description TEXT NULL,
    settings JSON NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_organization_company FOREIGN KEY (company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_organization_code ON organization(code);
CREATE UNIQUE INDEX idx_organization_slug ON organization(slug);
CREATE INDEX idx_organization_type ON organization(type);
CREATE INDEX idx_organization_status ON organization(status);

-- Agregar FK de users.last_organization_id
ALTER TABLE users
ADD CONSTRAINT fk_users_last_organization FOREIGN KEY (last_organization_id) 
    REFERENCES organization(id) ON DELETE SET NULL;

-- =========================
-- ZONE (Zonas dentro de una organización)
-- =========================
CREATE TABLE IF NOT EXISTS zone (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_zone_organization FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_zone_org_code ON zone(organization_id, code);
CREATE INDEX idx_zone_organization_id ON zone(organization_id);

-- =========================
-- TOWER (Torres dentro de una zona - solo para CIUDADELA)
-- =========================
CREATE TABLE IF NOT EXISTS tower (
    id INT AUTO_INCREMENT PRIMARY KEY,
    zone_id INT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    floors_count INT NULL,
    description TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_tower_zone FOREIGN KEY (zone_id) REFERENCES zone(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_tower_zone_code ON tower(zone_id, code);
CREATE INDEX idx_tower_zone_id ON tower(zone_id);

-- =========================
-- UNIT (Unidad habitacional: Apartamento o Casa)
-- =========================
CREATE TABLE IF NOT EXISTS unit (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    zone_id INT NULL COMMENT 'NULL si org no usa zonas',
    tower_id INT NULL COMMENT 'NULL si es conjunto',
    code VARCHAR(50) NOT NULL,
    type ENUM('APARTMENT','HOUSE') NOT NULL,
    floor INT NULL COMMENT 'Solo apartamentos',
    area_sqm DECIMAL(10,2) NULL,
    bedrooms INT NULL,
    bathrooms INT NULL,
    parking_spots INT NULL,
    status ENUM('AVAILABLE','OCCUPIED','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_unit_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_unit_zone FOREIGN KEY (zone_id) REFERENCES zone(id) ON DELETE SET NULL,
    CONSTRAINT fk_unit_tower FOREIGN KEY (tower_id) REFERENCES tower(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_unit_org_code ON unit(organization_id, code);
CREATE INDEX idx_unit_organization_id ON unit(organization_id);
CREATE INDEX idx_unit_zone_id ON unit(zone_id);
CREATE INDEX idx_unit_tower_id ON unit(tower_id);
CREATE INDEX idx_unit_type ON unit(type);
CREATE INDEX idx_unit_status ON unit(status);

-- =========================
-- MODULES (Sistema multi-tenant)
-- =========================
CREATE TABLE IF NOT EXISTS modules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'ATLAS_CORE, VISIT_CONTROL, etc',
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_modules_code ON modules(code);
CREATE INDEX idx_modules_is_active ON modules(is_active);

-- =========================
-- ORGANIZATION_MODULES (Módulos habilitados por organización)
-- =========================
CREATE TABLE IF NOT EXISTS organization_modules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    module_id INT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    settings JSON NULL COMMENT 'Configuraciones específicas del módulo',
    enabled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_org_modules_organization FOREIGN KEY (organization_id) 
        REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT fk_org_modules_module FOREIGN KEY (module_id) 
        REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT uq_org_modules UNIQUE (organization_id, module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_org_modules_organization_id ON organization_modules(organization_id);
CREATE INDEX idx_org_modules_module_id ON organization_modules(module_id);
CREATE INDEX idx_org_modules_is_enabled ON organization_modules(is_enabled);

-- =========================
-- PERMISSIONS (Permisos granulares)
-- =========================
CREATE TABLE IF NOT EXISTS permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE COMMENT 'UNITS_CREATE, VISITS_APPROVE, etc',
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    module_code VARCHAR(50) NULL COMMENT 'ATLAS_CORE, VISIT_CONTROL, NULL=global',
    resource VARCHAR(50) NOT NULL COMMENT 'units, visits, zones, etc',
    action VARCHAR(50) NOT NULL COMMENT 'CREATE, READ, UPDATE, DELETE, MANAGE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_permissions_code ON permissions(code);
CREATE INDEX idx_permissions_module_code ON permissions(module_code);
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_action ON permissions(action);

-- =========================
-- ROLE_PERMISSIONS (Relación N:N roles-permisos)
-- =========================
CREATE TABLE IF NOT EXISTS role_permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) 
        REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) 
        REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- =========================
-- USER_ORGANIZATIONS (Membresía multi-tenant)
-- =========================
CREATE TABLE IF NOT EXISTS user_organizations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    organization_id INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_organizations_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_organizations_organization FOREIGN KEY (organization_id) 
        REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_organizations UNIQUE (user_id, organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_organizations_user_id ON user_organizations(user_id);
CREATE INDEX idx_user_organizations_organization_id ON user_organizations(organization_id);
CREATE INDEX idx_user_organizations_status ON user_organizations(status);

-- =========================
-- USER_ROLES_MULTI (Roles por usuario por organización)
-- =========================
CREATE TABLE IF NOT EXISTS user_roles_multi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    organization_id INT NOT NULL,
    role_id INT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_roles_multi_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_multi_organization FOREIGN KEY (organization_id) 
        REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_multi_role FOREIGN KEY (role_id) 
        REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_roles_multi UNIQUE (user_id, organization_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_roles_multi_user_id ON user_roles_multi(user_id);
CREATE INDEX idx_user_roles_multi_organization_id ON user_roles_multi(organization_id);
CREATE INDEX idx_user_roles_multi_role_id ON user_roles_multi(role_id);
