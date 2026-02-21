-- Migración: Extender tipos de invitación y hacer email nullable
-- Fecha: 2025-01-15
-- Descripción: Agrega OWNER_SELF_REGISTER y RESIDENT_INVITE al ENUM de type,
--              y permite email NULL para invitaciones genéricas (sin email específico).

-- Agregar nuevos tipos de invitación al ENUM
ALTER TABLE invitations
    MODIFY COLUMN type ENUM(
        'ORG_MEMBER',
        'UNIT_OWNER',
        'UNIT_TENANT',
        'UNIT_FAMILY',
        'OWNER_INVITATION',
        'OWNER_SELF_REGISTER',
        'RESIDENT_INVITE'
    ) NOT NULL;

-- Permitir email NULL para invitaciones genéricas (ej: links de auto-registro de propietarios)
ALTER TABLE invitations
    MODIFY COLUMN email VARCHAR(255) NULL;
