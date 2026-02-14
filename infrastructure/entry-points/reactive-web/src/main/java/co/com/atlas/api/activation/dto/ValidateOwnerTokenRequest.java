package co.com.atlas.api.activation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para validar un token de invitación de propietario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateOwnerTokenRequest {
    
    /**
     * Token de invitación recibido por correo.
     */
    private String token;
}
