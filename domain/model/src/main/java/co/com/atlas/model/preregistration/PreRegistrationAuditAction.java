package co.com.atlas.model.preregistration;

/**
 * Acciones auditables en el flujo de pre-registro.
 */
public enum PreRegistrationAuditAction {
    /**
     * Token de pre-registro creado.
     */
    CREATED,
    
    /**
     * Email de activación enviado.
     */
    EMAIL_SENT,
    
    /**
     * Token activado exitosamente.
     */
    ACTIVATED,
    
    /**
     * Token expirado por tiempo.
     */
    EXPIRED,
    
    /**
     * Token revocado manualmente.
     */
    REVOKED,
    
    /**
     * Email de activación reenviado.
     */
    RESENT
}
