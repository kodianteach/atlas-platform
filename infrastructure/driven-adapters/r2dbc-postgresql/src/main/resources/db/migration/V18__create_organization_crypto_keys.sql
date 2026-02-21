-- ============================================================================
-- V18: Tabla de claves criptográficas por organización
-- Historia #5: Enrolamiento del Dispositivo de Portería
--
-- Almacena pares de claves EdDSA (Ed25519) por organización.
-- La clave pública se entrega al dispositivo de portería para verificar
-- firmas offline de QRs de autorización.
-- La clave privada se almacena cifrada con AES-256/GCM (master key).
-- ============================================================================

CREATE TABLE IF NOT EXISTS organization_crypto_keys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL COMMENT 'Organización dueña de las claves',
    algorithm VARCHAR(20) NOT NULL DEFAULT 'Ed25519' COMMENT 'Algoritmo de firma digital',
    key_id VARCHAR(100) NOT NULL COMMENT 'Key Identifier (kid) único UUID',
    public_key_jwk TEXT NOT NULL COMMENT 'Clave pública en formato JWK',
    private_key_encrypted TEXT NOT NULL COMMENT 'Clave privada cifrada con AES-256/GCM + master key',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Si la clave está activa para uso',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rotated_at TIMESTAMP NULL COMMENT 'Fecha de rotación (NULL si nunca rotada)',

    UNIQUE INDEX idx_org_crypto_key_id (key_id),
    INDEX idx_org_crypto_org_id (organization_id),
    INDEX idx_org_crypto_active (organization_id, is_active),

    CONSTRAINT fk_org_crypto_organization FOREIGN KEY (organization_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
