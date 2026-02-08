package co.com.atlas.model.visit;

/**
 * Estados de una solicitud de visita.
 */
public enum VisitStatus {
    /**
     * Pendiente de aprobación
     */
    PENDING,
    
    /**
     * Aprobada
     */
    APPROVED,
    
    /**
     * Rechazada
     */
    REJECTED,
    
    /**
     * Expirada
     */
    EXPIRED,
    
    /**
     * Cancelada
     */
    CANCELLED
}
