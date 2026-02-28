-- V22: Add branding fields to organization_configuration
-- Story #10: Configuración de Branding, Theming Dinámico y Navegación Unificada por Rol

ALTER TABLE organization_configuration
    ADD COLUMN logo_data MEDIUMBLOB NULL,
    ADD COLUMN logo_content_type VARCHAR(50) NULL,
    ADD COLUMN dominant_color VARCHAR(7) NULL,
    ADD COLUMN secondary_color VARCHAR(7) NULL,
    ADD COLUMN accent_color VARCHAR(7) NULL;
