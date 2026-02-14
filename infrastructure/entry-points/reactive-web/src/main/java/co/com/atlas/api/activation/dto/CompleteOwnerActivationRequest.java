package co.com.atlas.api.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para completar la activación de propietario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para completar activación de propietario")
public class CompleteOwnerActivationRequest {
    
    @Schema(description = "Token de invitación", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private String token;
    
    @Schema(description = "Contraseña elegida por el propietario", example = "SecurePass123!", required = true)
    private String password;
    
    @Schema(description = "Confirmación de contraseña", example = "SecurePass123!", required = true)
    private String confirmPassword;
}
