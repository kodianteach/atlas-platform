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
    OWNER_INVITATION
}
