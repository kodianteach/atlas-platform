package co.com.atlas.model.userunit;

/**
 * Tipos de propiedad en una unidad.
 */
public enum OwnershipType {
    /**
     * Propietario de la unidad
     */
    OWNER,
    
    /**
     * Arrendatario/Inquilino
     */
    TENANT,
    
    /**
     * Familiar del propietario/arrendatario
     */
    FAMILY,
    
    /**
     * Invitado temporal
     */
    GUEST
}
