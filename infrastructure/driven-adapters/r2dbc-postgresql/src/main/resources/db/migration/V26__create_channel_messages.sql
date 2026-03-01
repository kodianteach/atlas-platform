-- ============================================================================
-- V26: Create channel_messages and message_read_status tables (HU #13)
-- Canal de Mensajería Privada: Admin ↔ Porteros
-- MySQL 8.0 syntax
-- ============================================================================

CREATE TABLE IF NOT EXISTS channel_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    sender_id INT NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_channel_messages_org FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_channel_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    INDEX idx_channel_messages_org_id (organization_id),
    INDEX idx_channel_messages_created_at (created_at),
    INDEX idx_channel_messages_org_created (organization_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS message_read_status (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT NOT NULL,
    user_id INT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_read_msg FOREIGN KEY (message_id) REFERENCES channel_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_read_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_message_read_status UNIQUE (message_id, user_id),
    INDEX idx_message_read_message_id (message_id),
    INDEX idx_message_read_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
