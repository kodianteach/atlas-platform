package co.com.atlas.model.access;

/**
 * Resultados posibles del escaneo de código.
 */
public enum ScanResult {
    /**
     * Código válido - acceso permitido
     */
    VALID,
    
    /**
     * Código inválido o no encontrado
     */
    INVALID,
    
    /**
     * Código expirado por tiempo
     */
    EXPIRED,
    
    /**
     * Código ya usado (límite de entradas)
     */
    ALREADY_USED,
    
    /**
     * Código revocado
     */
    REVOKED
}
