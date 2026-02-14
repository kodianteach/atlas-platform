package co.com.atlas.api.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para completar la activación de propietario.
 * Incluye datos personales y contraseña.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para completar activación de propietario")
public class CompleteOwnerActivationRequest {
    
    @Schema(description = "Token de invitación", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private String token;
    
    // Datos personales del propietario
    @Schema(description = "Nombres completos", example = "Juan Carlos Pérez", required = true)
    private String names;
    
    @Schema(description = "Teléfono de contacto", example = "+57 300 123 4567")
    private String phone;
    
    @Schema(description = "Tipo de documento (CC, CE, NIT, PASSPORT)", example = "CC", required = true)
    private String documentType;
    
    @Schema(description = "Número de documento", example = "1234567890", required = true)
    private String documentNumber;
    
    // Contraseña
    @Schema(description = "Contraseña elegida por el propietario", example = "SecurePass123!", required = true)
    private String password;
    
    @Schema(description = "Confirmación de contraseña", example = "SecurePass123!", required = true)
    private String confirmPassword;
}
