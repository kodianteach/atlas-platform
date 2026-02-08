package co.com.atlas.api.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para activación de cuenta de administrador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivateAdminRequest {
    
    /**
     * Token de activación recibido por email.
     */
    private String token;
    
    /**
     * Email del usuario.
     */
    private String email;
    
    /**
     * Contraseña temporal recibida por email.
     */
    private String currentPassword;
    
    /**
     * Nueva contraseña que el usuario desea establecer.
     */
    private String newPassword;
}
