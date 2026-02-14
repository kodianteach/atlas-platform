-- =============================================
-- Flyway Migration V6
-- Atlas Platform - Módulo de Vehículos
-- Gestión y validación de vehículos por vivienda (Unit = APARTMENT | HOUSE)
-- =============================================

-- =========================
-- ALTER UNIT: Agregar cupo máximo de vehículos por vivienda
-- Separado de parking_spots (infraestructura física)
-- =========================
ALTER TABLE unit ADD COLUMN max_vehicles INT NOT NULL DEFAULT 2 AFTER parking_spots;

-- =========================
-- VEHICLES (Vehículos asociados a viviendas)
-- Cada vehículo pertenece a una Unit (casa o apartamento)
-- =========================
CREATE TABLE IF NOT EXISTS vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    unit_id INT NOT NULL COMMENT 'Vivienda (casa o apartamento) a la que pertenece el vehículo',
    organization_id INT NOT NULL COMMENT 'Desnormalizado para queries rápidas de validación por guarda',
    plate VARCHAR(20) NOT NULL COMMENT 'Placa del vehículo (normalizada: uppercase, trim)',
    vehicle_type ENUM('CAR','MOTORCYCLE','BICYCLE','OTHER') NOT NULL DEFAULT 'CAR',
    brand VARCHAR(80) NULL,
    model VARCHAR(80) NULL,
    color VARCHAR(40) NULL,
    owner_name VARCHAR(160) NULL COMMENT 'Nombre del responsable del vehículo',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'TRUE=permitida para ingreso, FALSE=bloqueada',
    registered_by INT NULL COMMENT 'Usuario que registró el vehículo',
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_vehicles_unit FOREIGN KEY (unit_id) REFERENCES unit(id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicles_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_vehicles_registered_by FOREIGN KEY (registered_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Placa única por organización (multi-tenant: misma placa puede existir en orgs distintas)
CREATE UNIQUE INDEX idx_vehicles_org_plate ON vehicles(organization_id, plate);
-- Índices para búsquedas frecuentes
CREATE INDEX idx_vehicles_unit_id ON vehicles(unit_id);
CREATE INDEX idx_vehicles_organization_id ON vehicles(organization_id);
CREATE INDEX idx_vehicles_plate ON vehicles(plate);
CREATE INDEX idx_vehicles_is_active ON vehicles(is_active);
CREATE INDEX idx_vehicles_vehicle_type ON vehicles(vehicle_type);

-- =========================
-- MÓDULO VEHICLE_CONTROL
-- =========================
INSERT INTO modules (code, name, description, is_active) VALUES
('VEHICLE_CONTROL', 'Control de Vehículos', 'Módulo de gestión y validación de vehículos por vivienda', TRUE);

-- =========================
-- PERMISOS GRANULARES PARA VEHÍCULOS
-- =========================
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('VEHICLES_CREATE', 'Registrar Vehículos', 'Permite registrar vehículos a una vivienda', 'VEHICLE_CONTROL', 'vehicles', 'CREATE'),
('VEHICLES_READ', 'Ver Vehículos', 'Permite ver vehículos registrados', 'VEHICLE_CONTROL', 'vehicles', 'READ'),
('VEHICLES_UPDATE', 'Editar Vehículos', 'Permite modificar datos de vehículos', 'VEHICLE_CONTROL', 'vehicles', 'UPDATE'),
('VEHICLES_DELETE', 'Eliminar Vehículos', 'Permite eliminar vehículos', 'VEHICLE_CONTROL', 'vehicles', 'DELETE'),
('VEHICLES_MANAGE', 'Gestionar Vehículos', 'Control total de vehículos (bulk, sync)', 'VEHICLE_CONTROL', 'vehicles', 'MANAGE'),
('VEHICLES_VALIDATE', 'Validar Placas', 'Permite validar ingreso por placa (guardas)', 'VEHICLE_CONTROL', 'vehicles', 'VALIDATE');

-- =========================
-- ASIGNACIÓN DE PERMISOS A ROLES
-- =========================

-- SUPER_ADMIN: todos los permisos de vehículos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND p.module_code = 'VEHICLE_CONTROL';

-- ADMIN_ATLAS: gestión completa de vehículos en su organización
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'ADMIN_ATLAS'
AND p.code IN ('VEHICLES_CREATE', 'VEHICLES_READ', 'VEHICLES_UPDATE', 'VEHICLES_DELETE', 'VEHICLES_MANAGE', 'VEHICLES_VALIDATE');

-- OWNER: CRUD de vehículos de su propia vivienda
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'OWNER'
AND p.code IN ('VEHICLES_CREATE', 'VEHICLES_READ', 'VEHICLES_UPDATE', 'VEHICLES_DELETE');

-- TENANT: CRUD de vehículos de su propia vivienda
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'TENANT'
AND p.code IN ('VEHICLES_CREATE', 'VEHICLES_READ', 'VEHICLES_UPDATE', 'VEHICLES_DELETE');

-- FAMILY: solo lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'FAMILY' AND p.code = 'VEHICLES_READ';

-- SECURITY: lectura + validación de placas (API de guarda)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p
WHERE r.code = 'SECURITY'
AND p.code IN ('VEHICLES_READ', 'VEHICLES_VALIDATE');

-- =========================
-- HABILITAR MÓDULO PARA ORGANIZACIÓN DEMO
-- =========================
INSERT INTO organization_modules (organization_id, module_id, is_enabled)
SELECT o.id, m.id, TRUE FROM organization o, modules m
WHERE o.code = 'CIUDADELA-001' AND m.code = 'VEHICLE_CONTROL';

-- =========================
-- DATOS DE PRUEBA: Vehículos asociados a unidades existentes
-- =========================

-- Vehículo activo para la unidad 101 (Apartamento en Torre 1)
INSERT INTO vehicles (unit_id, organization_id, plate, vehicle_type, brand, color, owner_name, is_active, notes)
SELECT u.id, u.organization_id, 'ABC123', 'CAR', 'Renault', 'Blanco', 'Admin Atlas', TRUE, 'Vehículo de prueba - activo'
FROM unit u WHERE u.code = '101';

-- Vehículo inactivo para la misma unidad 101
INSERT INTO vehicles (unit_id, organization_id, plate, vehicle_type, brand, color, owner_name, is_active, notes)
SELECT u.id, u.organization_id, 'XYZ789', 'MOTORCYCLE', 'Honda', 'Negro', 'Admin Atlas', FALSE, 'Moto inactiva de prueba'
FROM unit u WHERE u.code = '101';

-- Vehículo activo para la unidad 102
INSERT INTO vehicles (unit_id, organization_id, plate, vehicle_type, brand, color, owner_name, is_active, notes)
SELECT u.id, u.organization_id, 'DEF456', 'CAR', 'Chevrolet', 'Gris', 'Vecino Demo', TRUE, 'Vehículo de prueba unidad 102'
FROM unit u WHERE u.code = '102';
