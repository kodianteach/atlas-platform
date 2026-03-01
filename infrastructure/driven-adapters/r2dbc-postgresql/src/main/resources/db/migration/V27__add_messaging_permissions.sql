-- V27: Add messaging permissions (HU #13)
-- MySQL syntax

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('MESSAGES_SEND', 'Enviar Mensajes', 'Permite enviar mensajes en el canal de mensajería', 'ATLAS_CORE', 'messages', 'SEND');

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('MESSAGES_READ', 'Leer Mensajes', 'Permite leer mensajes del canal de mensajería', 'ATLAS_CORE', 'messages', 'READ');

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('MESSAGES_EDIT_OWN', 'Editar Mensajes Propios', 'Permite editar mensajes propios en el canal', 'ATLAS_CORE', 'messages', 'EDIT_OWN');

INSERT IGNORE INTO permissions (code, name, description, module_code, resource, action) VALUES ('MESSAGES_DELETE_OWN', 'Eliminar Mensajes Propios', 'Permite eliminar mensajes propios en el canal', 'ATLAS_CORE', 'messages', 'DELETE_OWN');

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at) SELECT r.id, p.id, NOW() FROM role r CROSS JOIN permissions p WHERE r.code = 'ADMIN_ATLAS' AND p.code IN ('MESSAGES_SEND', 'MESSAGES_READ', 'MESSAGES_EDIT_OWN', 'MESSAGES_DELETE_OWN');

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at) SELECT r.id, p.id, NOW() FROM role r CROSS JOIN permissions p WHERE r.code = 'ADMIN' AND p.code IN ('MESSAGES_SEND', 'MESSAGES_READ', 'MESSAGES_EDIT_OWN', 'MESSAGES_DELETE_OWN');

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at) SELECT r.id, p.id, NOW() FROM role r CROSS JOIN permissions p WHERE r.code = 'PORTERO_GENERAL' AND p.code IN ('MESSAGES_SEND', 'MESSAGES_READ', 'MESSAGES_EDIT_OWN', 'MESSAGES_DELETE_OWN');

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at) SELECT r.id, p.id, NOW() FROM role r CROSS JOIN permissions p WHERE r.code = 'PORTERO_DELIVERY' AND p.code IN ('MESSAGES_SEND', 'MESSAGES_READ', 'MESSAGES_EDIT_OWN', 'MESSAGES_DELETE_OWN');
