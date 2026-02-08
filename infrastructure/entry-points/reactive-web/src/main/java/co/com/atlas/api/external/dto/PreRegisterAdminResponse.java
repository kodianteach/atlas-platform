package co.com.atlas.api.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response del pre-registro de administrador.
 * No expone la contraseña temporal (solo se envía por email).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreRegisterAdminResponse {
    
    /**
     * ID del usuario creado.
     */
    private Long userId;
    
    /**
     * ID del token de activación.
     */
    private Long tokenId;
    
    /**
     * Email del usuario.
     */
    private String email;
    
    /**
     * Nombre del usuario.
     */
    private String names;
    
    /**
     * Fecha de expiración del token.
     */
    private Instant expiresAt;
    
    /**
     * Mensaje informativo.
     */
    private String message;
}
