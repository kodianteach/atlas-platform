-- V25: Add admin panel permissions (HU #12)
-- MySQL syntax

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('POSTS_MANAGE', 'Gestionar Publicaciones', 'Gestion completa de publicaciones (admin)', 'ATLAS_CORE', 'posts', 'MANAGE');

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('COMMENTS_READ', 'Ver Comentarios', 'Lectura de comentarios', 'ATLAS_CORE', 'comments', 'READ');

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('COMMENTS_MANAGE', 'Gestionar Comentarios', 'Gestion completa de comentarios (admin)', 'ATLAS_CORE', 'comments', 'MANAGE');

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('POLLS_MANAGE', 'Gestionar Encuestas', 'Gestion completa de encuestas (admin)', 'ATLAS_CORE', 'polls', 'MANAGE');

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at) SELECT r.id, p.id, NOW() FROM role r CROSS JOIN permissions p WHERE r.code = 'ADMIN_ATLAS' AND p.code IN ('POSTS_MANAGE', 'COMMENTS_READ', 'COMMENTS_MANAGE', 'POLLS_MANAGE');
