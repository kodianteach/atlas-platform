package co.com.atlas.model.preregistration;

/**
 * Estados posibles de un token de activación de administrador.
 */
public enum ActivationTokenStatus {
    /**
     * Token pendiente de ser usado.
     */
    PENDING,
    
    /**
     * Token consumido exitosamente.
     */
    CONSUMED,
    
    /**
     * Token expirado por tiempo.
     */
    EXPIRED,
    
    /**
     * Token revocado manualmente por un operador.
     */
    REVOKED
}
