-- =============================================
-- Flyway Migration V12
-- Agregar permiso VISITS_CREATE al rol ADMIN_ATLAS
-- =============================================

-- ADMIN_ATLAS necesita poder crear visitas además de leerlas y aprobarlas
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'ADMIN_ATLAS' 
AND p.code = 'VISITS_CREATE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
