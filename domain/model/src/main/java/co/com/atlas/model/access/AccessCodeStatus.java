package co.com.atlas.model.access;

/**
 * Estados de un código de acceso.
 */
public enum AccessCodeStatus {
    /**
     * Código activo y válido
     */
    ACTIVE,
    
    /**
     * Código usado (límite de entradas alcanzado)
     */
    USED,
    
    /**
     * Código expirado por tiempo
     */
    EXPIRED,
    
    /**
     * Código revocado manualmente
     */
    REVOKED
}
