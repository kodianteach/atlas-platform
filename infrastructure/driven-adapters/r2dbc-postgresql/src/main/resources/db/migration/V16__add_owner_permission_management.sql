-- V16: Add owner permission management configuration field
-- Story #9: Invitación y Registro de Propietarios + Invitación de Residentes

ALTER TABLE organization_configuration
    ADD COLUMN enable_owner_permission_management BOOLEAN DEFAULT FALSE;
