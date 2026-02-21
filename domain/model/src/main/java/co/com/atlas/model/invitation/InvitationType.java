package co.com.atlas.model.invitation;

/**
 * Tipos de invitación soportados.
 */
public enum InvitationType {
    /**
     * Miembro a nivel de organización
     */
    ORG_MEMBER,
    
    /**
     * Propietario de una unidad
     */
    UNIT_OWNER,
    
    /**
     * Arrendatario de una unidad
     */
    UNIT_TENANT,
    
    /**
     * Familiar en una unidad
     */
    UNIT_FAMILY,
    
    /**
     * Invitación de propietario creado junto con la unidad.
     * Usado en flujo de distribución/carga masiva de unidades.
     */
    OWNER_INVITATION,
    
    /**
     * Invitación genérica para que un propietario se auto-registre.
     * Generada por ADMIN_ATLAS. El propietario selecciona su unidad al registrarse.
     */
    OWNER_SELF_REGISTER,
    
    /**
     * Invitación para un residente de una unidad específica.
     * Generada por OWNER. Incluye unitId y permisos opcionales.
     */
    RESIDENT_INVITE
}
