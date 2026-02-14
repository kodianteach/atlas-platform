package co.com.atlas.model.config.gateways;

import reactor.core.publisher.Mono;

/**
 * Gateway para acceso a configuración del sistema.
 * Lee valores de la tabla system_configuration.
 */
public interface SystemConfigurationRepository {
    
    /**
     * Obtiene un valor de configuración como String.
     * 
     * @param key clave de configuración
     * @return valor o empty si no existe
     */
    Mono<String> getString(String key);
    
    /**
     * Obtiene un valor de configuración como Integer.
     * 
     * @param key clave de configuración
     * @param defaultValue valor por defecto si no existe
     * @return valor o defaultValue
     */
    Mono<Integer> getInteger(String key, Integer defaultValue);
    
    /**
     * Obtiene un valor de configuración como Boolean.
     * 
     * @param key clave de configuración
     * @param defaultValue valor por defecto si no existe
     * @return valor o defaultValue
     */
    Mono<Boolean> getBoolean(String key, Boolean defaultValue);
    
    // Constantes para claves de configuración
    
    /** Días de expiración para invitaciones (default: 7) */
    String INVITATION_EXPIRATION_DAYS = "INVITATION_EXPIRATION_DAYS";
    
    /** Máximo de reintentos de envío de invitación (default: 3) */
    String INVITATION_MAX_RETRY_COUNT = "INVITATION_MAX_RETRY_COUNT";
    
    /** Horas mínimas entre reenvíos (default: 1) */
    String INVITATION_RESEND_COOLDOWN_HOURS = "INVITATION_RESEND_COOLDOWN_HOURS";
    
    /** Días de expiración para invitaciones de propietario (default: 7) */
    String OWNER_INVITATION_EXPIRATION_DAYS = "OWNER_INVITATION_EXPIRATION_DAYS";
}
