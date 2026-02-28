-- =============================================
-- Flyway Migration V23
-- Atlas Platform - Notificaciones In-App
-- HU #11: Canal de Difusión
-- =============================================

CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    user_id INT NULL COMMENT 'NULL para notificaciones broadcast a toda la organización',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL COMMENT 'POST_PUBLISHED, POLL_ACTIVATED, etc.',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(50) NULL COMMENT 'POST, POLL, etc.',
    entity_id INT NULL COMMENT 'ID de la entidad relacionada',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_organization FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_org_user ON notifications(organization_id, user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_entity ON notifications(entity_type, entity_id);
