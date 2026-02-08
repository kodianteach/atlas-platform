package co.com.atlas.model.auth;

/**
 * Estados posibles de un usuario en el sistema.
 */
public enum UserStatus {
    /**
     * Usuario activo con acceso completo al sistema.
     */
    ACTIVE,
    
    /**
     * Usuario pre-registrado por un operador.
     * Tiene credenciales temporales pero no ha activado su cuenta.
     */
    PRE_REGISTERED,
    
    /**
     * Usuario con token de activación pendiente.
     * Ha recibido el email pero no ha completado el proceso.
     */
    PENDING_ACTIVATION,
    
    /**
     * Usuario que ha activado su cuenta pero no ha completado el onboarding.
     * Puede iniciar sesión pero tiene funcionalidad limitada hasta crear company/organization.
     */
    ACTIVATED,
    
    /**
     * Usuario suspendido por un administrador.
     * No puede iniciar sesión.
     */
    SUSPENDED
}
