package co.com.atlas.model.preregistration;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para token de activación de administrador pre-registrado.
 * 
 * Este token se genera cuando un operador crea un pre-registro de administrador
 * y se envía por correo junto con credenciales temporales.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class AdminActivationToken {
    /**
     * ID único del token.
     */
    private Long id;
    
    /**
     * ID del usuario asociado al token.
     */
    private Long userId;
    
    /**
     * Hash SHA-256 del token (nunca se almacena el token en claro).
     */
    private String tokenHash;
    
    /**
     * Hash BCrypt de la contraseña temporal enviada al usuario.
     * Se usa para validar las credenciales durante la activación.
     */
    private String initialPasswordHash;
    
    /**
     * Fecha de expiración del token.
     */
    private Instant expiresAt;
    
    /**
     * Fecha en que se consumió el token (null si no se ha consumido).
     */
    private Instant consumedAt;
    
    /**
     * Estado actual del token.
     */
    private ActivationTokenStatus status;
    
    /**
     * ID del operador que creó el pre-registro.
     */
    private Long createdBy;
    
    /**
     * IP desde donde se creó el pre-registro.
     */
    private String ipAddress;
    
    /**
     * User-Agent del cliente que creó el pre-registro.
     */
    private String userAgent;
    
    /**
     * IP desde donde se activó el token.
     */
    private String activationIp;
    
    /**
     * User-Agent del cliente que activó el token.
     */
    private String activationUserAgent;
    
    /**
     * Metadata adicional (JSON serializado).
     */
    private String metadata;
    
    /**
     * Fecha de creación.
     */
    private Instant createdAt;
    
    /**
     * Fecha de última actualización.
     */
    private Instant updatedAt;
    
    /**
     * Verifica si el token ha expirado.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Verifica si el token es válido para su uso.
     */
    public boolean isValid() {
        return status == ActivationTokenStatus.PENDING && !isExpired();
    }
}
