-- V13: Organization Configuration table
CREATE TABLE IF NOT EXISTS organization_configuration (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    max_units_per_distribution INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_org_config_organization FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT uq_org_config_organization UNIQUE (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

