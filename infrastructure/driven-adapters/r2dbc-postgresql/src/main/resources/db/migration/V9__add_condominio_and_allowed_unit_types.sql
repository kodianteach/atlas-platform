-- =========================
-- V9: Agregar CONDOMINIO y campo allowed_unit_types
-- =========================
-- Descripción: 
--   1. Agregar CONDOMINIO como tercer tipo de organización
--   2. Agregar campo allowed_unit_types para controlar qué tipos de unidades permite cada org
--   3. Ciudadela y Conjunto ahora pueden tener casas o departamentos según configuración
--   4. Condominio solo permite casas

-- =========================
-- 1. Modificar ENUM organization.type para agregar CONDOMINIO
-- =========================
-- En MySQL, para modificar un ENUM hay que hacer ALTER COLUMN
ALTER TABLE organization 
MODIFY COLUMN type ENUM('CIUDADELA','CONJUNTO','CONDOMINIO') NOT NULL 
COMMENT 'Tipo de organización residencial: CIUDADELA (zonas→torres→apts/casas), CONJUNTO (zonas→apts/casas), CONDOMINIO (zonas→casas)';

-- =========================
-- 2. Agregar columna allowed_unit_types
-- =========================
-- Valores posibles: 'HOUSE', 'APARTMENT', 'HOUSE,APARTMENT'
ALTER TABLE organization 
ADD COLUMN allowed_unit_types VARCHAR(50) NOT NULL DEFAULT 'HOUSE,APARTMENT' 
COMMENT 'Tipos de unidades permitidas: HOUSE, APARTMENT, o ambos separados por coma'
AFTER uses_zones;

-- =========================
-- 3. Actualizar datos existentes según tipo actual
-- =========================
-- CIUDADELA existentes: por defecto solo tenían APARTMENT
UPDATE organization 
SET allowed_unit_types = 'APARTMENT' 
WHERE type = 'CIUDADELA';

-- CONJUNTO existentes: por defecto solo tenían HOUSE
UPDATE organization 
SET allowed_unit_types = 'HOUSE' 
WHERE type = 'CONJUNTO';

-- Nota: Los nuevos CIUDADELA y CONJUNTO podrán configurarse con ambos tipos
-- Los CONDOMINIO siempre serán 'HOUSE' (validado en la aplicación)

-- =========================
-- 4. Crear índice para allowed_unit_types
-- =========================
CREATE INDEX idx_organization_allowed_unit_types ON organization(allowed_unit_types);
