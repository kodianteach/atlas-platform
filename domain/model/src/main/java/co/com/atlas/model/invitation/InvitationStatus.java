package co.com.atlas.model.invitation;

/**
 * Estados de una invitación.
 */
public enum InvitationStatus {
    /**
     * Pendiente de aceptación
     */
    PENDING,
    
    /**
     * Aceptada por el usuario
     */
    ACCEPTED,
    
    /**
     * Expirada (tiempo límite alcanzado)
     */
    EXPIRED,
    
    /**
     * Cancelada por el administrador
     */
    CANCELLED
}
