-- =============================================
-- Flyway Migration V3
-- Atlas Platform - Datos Semilla
-- Roles, Permisos y Módulos iniciales
-- =============================================

-- =========================
-- MÓDULOS DEL SISTEMA
-- =========================
INSERT INTO modules (code, name, description, is_active) VALUES
('ATLAS_CORE', 'Atlas Core', 'Módulo principal de gestión de organizaciones residenciales', TRUE),
('VISIT_CONTROL', 'Control de Visitas', 'Módulo de solicitudes y aprobación de visitas', TRUE),
('ACCESS_CONTROL', 'Control de Acceso', 'Módulo de generación y validación de códigos/QR', TRUE);

-- =========================
-- ROLES DEL SISTEMA
-- =========================
INSERT INTO role (name, code, description, module_code, is_system) VALUES
-- Roles globales
('Super Administrador', 'SUPER_ADMIN', 'Acceso total al sistema', NULL, TRUE),

-- Roles de Atlas Core
('Administrador Atlas', 'ADMIN_ATLAS', 'Administrador de organización residencial', 'ATLAS_CORE', TRUE),
('Propietario', 'OWNER', 'Propietario de unidad', 'ATLAS_CORE', TRUE),
('Arrendatario', 'TENANT', 'Arrendatario de unidad', 'ATLAS_CORE', TRUE),
('Familiar', 'FAMILY', 'Familiar o residente secundario', 'ATLAS_CORE', TRUE),
('Invitado', 'GUEST', 'Invitado temporal', 'ATLAS_CORE', TRUE),

-- Roles de Control de Acceso
('Seguridad', 'SECURITY', 'Personal de seguridad/portería', 'ACCESS_CONTROL', TRUE);

-- =========================
-- PERMISOS GRANULARES
-- =========================

-- Permisos de Organizaciones
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('ORGANIZATIONS_CREATE', 'Crear Organizaciones', 'Permite crear nuevas organizaciones', 'ATLAS_CORE', 'organizations', 'CREATE'),
('ORGANIZATIONS_READ', 'Ver Organizaciones', 'Permite ver información de organizaciones', 'ATLAS_CORE', 'organizations', 'READ'),
('ORGANIZATIONS_UPDATE', 'Editar Organizaciones', 'Permite modificar organizaciones', 'ATLAS_CORE', 'organizations', 'UPDATE'),
('ORGANIZATIONS_DELETE', 'Eliminar Organizaciones', 'Permite eliminar organizaciones', 'ATLAS_CORE', 'organizations', 'DELETE'),
('ORGANIZATIONS_MANAGE', 'Gestionar Organizaciones', 'Control total de organizaciones', 'ATLAS_CORE', 'organizations', 'MANAGE');

-- Permisos de Zonas
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('ZONES_CREATE', 'Crear Zonas', 'Permite crear zonas', 'ATLAS_CORE', 'zones', 'CREATE'),
('ZONES_READ', 'Ver Zonas', 'Permite ver zonas', 'ATLAS_CORE', 'zones', 'READ'),
('ZONES_UPDATE', 'Editar Zonas', 'Permite modificar zonas', 'ATLAS_CORE', 'zones', 'UPDATE'),
('ZONES_DELETE', 'Eliminar Zonas', 'Permite eliminar zonas', 'ATLAS_CORE', 'zones', 'DELETE');

-- Permisos de Torres
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('TOWERS_CREATE', 'Crear Torres', 'Permite crear torres', 'ATLAS_CORE', 'towers', 'CREATE'),
('TOWERS_READ', 'Ver Torres', 'Permite ver torres', 'ATLAS_CORE', 'towers', 'READ'),
('TOWERS_UPDATE', 'Editar Torres', 'Permite modificar torres', 'ATLAS_CORE', 'towers', 'UPDATE'),
('TOWERS_DELETE', 'Eliminar Torres', 'Permite eliminar torres', 'ATLAS_CORE', 'towers', 'DELETE');

-- Permisos de Unidades
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('UNITS_CREATE', 'Crear Unidades', 'Permite crear unidades', 'ATLAS_CORE', 'units', 'CREATE'),
('UNITS_READ', 'Ver Unidades', 'Permite ver unidades', 'ATLAS_CORE', 'units', 'READ'),
('UNITS_UPDATE', 'Editar Unidades', 'Permite modificar unidades', 'ATLAS_CORE', 'units', 'UPDATE'),
('UNITS_DELETE', 'Eliminar Unidades', 'Permite eliminar unidades', 'ATLAS_CORE', 'units', 'DELETE'),
('UNITS_MANAGE', 'Gestionar Unidades', 'Control total de unidades', 'ATLAS_CORE', 'units', 'MANAGE');

-- Permisos de Usuarios en Unidades
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('USER_UNITS_CREATE', 'Vincular Usuarios', 'Permite vincular usuarios a unidades', 'ATLAS_CORE', 'user_units', 'CREATE'),
('USER_UNITS_READ', 'Ver Usuarios de Unidad', 'Permite ver usuarios de unidades', 'ATLAS_CORE', 'user_units', 'READ'),
('USER_UNITS_UPDATE', 'Editar Vínculo Usuario-Unidad', 'Permite modificar vínculos', 'ATLAS_CORE', 'user_units', 'UPDATE'),
('USER_UNITS_DELETE', 'Desvincular Usuarios', 'Permite desvincular usuarios', 'ATLAS_CORE', 'user_units', 'DELETE');

-- Permisos de Invitaciones
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('INVITATIONS_CREATE', 'Crear Invitaciones', 'Permite crear invitaciones', 'ATLAS_CORE', 'invitations', 'CREATE'),
('INVITATIONS_READ', 'Ver Invitaciones', 'Permite ver invitaciones', 'ATLAS_CORE', 'invitations', 'READ'),
('INVITATIONS_CANCEL', 'Cancelar Invitaciones', 'Permite cancelar invitaciones', 'ATLAS_CORE', 'invitations', 'DELETE'),
('INVITATIONS_RESEND', 'Reenviar Invitaciones', 'Permite reenviar invitaciones', 'ATLAS_CORE', 'invitations', 'UPDATE');

-- Permisos de Visitas
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('VISITS_CREATE', 'Solicitar Visitas', 'Permite crear solicitudes de visita', 'VISIT_CONTROL', 'visits', 'CREATE'),
('VISITS_READ', 'Ver Visitas', 'Permite ver solicitudes de visita', 'VISIT_CONTROL', 'visits', 'READ'),
('VISITS_UPDATE', 'Editar Visitas', 'Permite modificar solicitudes', 'VISIT_CONTROL', 'visits', 'UPDATE'),
('VISITS_DELETE', 'Cancelar Visitas', 'Permite cancelar solicitudes', 'VISIT_CONTROL', 'visits', 'DELETE'),
('VISITS_APPROVE', 'Aprobar Visitas', 'Permite aprobar/rechazar solicitudes', 'VISIT_CONTROL', 'visits', 'MANAGE');

-- Permisos de Control de Acceso
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('ACCESS_CODES_READ', 'Ver Códigos de Acceso', 'Permite ver códigos generados', 'ACCESS_CONTROL', 'access_codes', 'READ'),
('ACCESS_CODES_VALIDATE', 'Validar Códigos', 'Permite escanear y validar códigos', 'ACCESS_CONTROL', 'access_codes', 'UPDATE'),
('ACCESS_CODES_REVOKE', 'Revocar Códigos', 'Permite revocar códigos activos', 'ACCESS_CONTROL', 'access_codes', 'DELETE'),
('ACCESS_LOG_READ', 'Ver Log de Accesos', 'Permite ver historial de escaneos', 'ACCESS_CONTROL', 'access_log', 'READ');

-- =========================
-- ASIGNACIÓN DE PERMISOS A ROLES
-- =========================

-- SUPER_ADMIN: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p WHERE r.code = 'SUPER_ADMIN';

-- ADMIN_ATLAS: gestión completa de su organización
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'ADMIN_ATLAS' 
AND p.code IN (
    'ORGANIZATIONS_READ', 'ORGANIZATIONS_UPDATE',
    'ZONES_CREATE', 'ZONES_READ', 'ZONES_UPDATE', 'ZONES_DELETE',
    'TOWERS_CREATE', 'TOWERS_READ', 'TOWERS_UPDATE', 'TOWERS_DELETE',
    'UNITS_CREATE', 'UNITS_READ', 'UNITS_UPDATE', 'UNITS_DELETE', 'UNITS_MANAGE',
    'USER_UNITS_CREATE', 'USER_UNITS_READ', 'USER_UNITS_UPDATE', 'USER_UNITS_DELETE',
    'INVITATIONS_CREATE', 'INVITATIONS_READ', 'INVITATIONS_CANCEL', 'INVITATIONS_RESEND',
    'VISITS_READ', 'VISITS_APPROVE',
    'ACCESS_CODES_READ', 'ACCESS_LOG_READ'
);

-- OWNER: gestión de su unidad
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'OWNER' 
AND p.code IN (
    'UNITS_READ',
    'USER_UNITS_CREATE', 'USER_UNITS_READ', 'USER_UNITS_UPDATE', 'USER_UNITS_DELETE',
    'INVITATIONS_CREATE', 'INVITATIONS_READ', 'INVITATIONS_CANCEL', 'INVITATIONS_RESEND',
    'VISITS_CREATE', 'VISITS_READ', 'VISITS_UPDATE', 'VISITS_DELETE', 'VISITS_APPROVE',
    'ACCESS_CODES_READ'
);

-- TENANT: gestión limitada
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'TENANT' 
AND p.code IN (
    'UNITS_READ',
    'USER_UNITS_READ',
    'INVITATIONS_CREATE', 'INVITATIONS_READ',
    'VISITS_CREATE', 'VISITS_READ', 'VISITS_UPDATE', 'VISITS_DELETE', 'VISITS_APPROVE',
    'ACCESS_CODES_READ'
);

-- FAMILY: permisos básicos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'FAMILY' 
AND p.code IN (
    'UNITS_READ',
    'USER_UNITS_READ',
    'VISITS_CREATE', 'VISITS_READ',
    'ACCESS_CODES_READ'
);

-- GUEST: solo lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'GUEST' 
AND p.code IN (
    'UNITS_READ',
    'VISITS_READ'
);

-- SECURITY: control de acceso
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'SECURITY' 
AND p.code IN (
    'VISITS_READ',
    'ACCESS_CODES_READ', 'ACCESS_CODES_VALIDATE',
    'ACCESS_LOG_READ'
);

-- =========================
-- USUARIO ADMIN DE PRUEBAS
-- =========================
INSERT INTO users (names, email, password_hash, is_active) VALUES
('Admin Atlas', 'admin@atlas.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', TRUE);

-- Asignar rol SUPER_ADMIN al usuario admin
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM users u, role r 
WHERE u.email = 'admin@atlas.com' AND r.code = 'SUPER_ADMIN';

-- =========================
-- COMPANY Y ORGANIZATION DE PRUEBAS
-- =========================
INSERT INTO company (name, slug, status) VALUES
('Atlas Demo Company', 'atlas-demo', 'ACTIVE');

INSERT INTO organization (company_id, code, name, slug, type, uses_zones, status) 
SELECT c.id, 'CIUDADELA-001', 'Ciudadela El Roble', 'ciudadela-el-roble', 'CIUDADELA', TRUE, 'ACTIVE'
FROM company c WHERE c.slug = 'atlas-demo';

-- Habilitar módulos para la organización
INSERT INTO organization_modules (organization_id, module_id, is_enabled)
SELECT o.id, m.id, TRUE 
FROM organization o, modules m 
WHERE o.code = 'CIUDADELA-001';

-- Vincular admin a la organización
INSERT INTO user_organizations (user_id, organization_id, status)
SELECT u.id, o.id, 'ACTIVE'
FROM users u, organization o 
WHERE u.email = 'admin@atlas.com' AND o.code = 'CIUDADELA-001';

-- Asignar rol ADMIN_ATLAS en la organización
INSERT INTO user_roles_multi (user_id, organization_id, role_id, is_primary)
SELECT u.id, o.id, r.id, TRUE
FROM users u, organization o, role r
WHERE u.email = 'admin@atlas.com' 
AND o.code = 'CIUDADELA-001' 
AND r.code = 'ADMIN_ATLAS';

-- Actualizar last_organization_id del admin
UPDATE users u
SET u.last_organization_id = (SELECT id FROM organization WHERE code = 'CIUDADELA-001')
WHERE u.email = 'admin@atlas.com';

-- =========================
-- DATOS DE PRUEBA: Zonas, Torres y Unidades
-- =========================

-- Zonas
INSERT INTO zone (organization_id, code, name, sort_order) 
SELECT o.id, 'ZONA-A', 'Zona A', 1 FROM organization o WHERE o.code = 'CIUDADELA-001';

INSERT INTO zone (organization_id, code, name, sort_order) 
SELECT o.id, 'ZONA-B', 'Zona B', 2 FROM organization o WHERE o.code = 'CIUDADELA-001';

-- Torres en Zona A
INSERT INTO tower (zone_id, code, name, floors_count, sort_order)
SELECT z.id, 'TORRE-1', 'Torre 1', 10, 1 FROM zone z WHERE z.code = 'ZONA-A';

INSERT INTO tower (zone_id, code, name, floors_count, sort_order)
SELECT z.id, 'TORRE-2', 'Torre 2', 10, 2 FROM zone z WHERE z.code = 'ZONA-A';

-- Unidades en Torre 1
INSERT INTO unit (organization_id, zone_id, tower_id, code, type, floor, status)
SELECT o.id, z.id, t.id, '101', 'APARTMENT', 1, 'AVAILABLE'
FROM organization o, zone z, tower t
WHERE o.code = 'CIUDADELA-001' AND z.code = 'ZONA-A' AND t.code = 'TORRE-1';

INSERT INTO unit (organization_id, zone_id, tower_id, code, type, floor, status)
SELECT o.id, z.id, t.id, '102', 'APARTMENT', 1, 'AVAILABLE'
FROM organization o, zone z, tower t
WHERE o.code = 'CIUDADELA-001' AND z.code = 'ZONA-A' AND t.code = 'TORRE-1';

INSERT INTO unit (organization_id, zone_id, tower_id, code, type, floor, status)
SELECT o.id, z.id, t.id, '201', 'APARTMENT', 2, 'AVAILABLE'
FROM organization o, zone z, tower t
WHERE o.code = 'CIUDADELA-001' AND z.code = 'ZONA-A' AND t.code = 'TORRE-1';
