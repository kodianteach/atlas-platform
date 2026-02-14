package co.com.atlas.api.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para aceptar invitación y registrar datos del propietario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para aceptar invitación de propietario")
public class AcceptInvitationRequest {
    
    @Schema(description = "Token de la invitación", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
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
    
    @Schema(description = "Contraseña", example = "SecurePass123!", required = true)
    private String password;
    
    @Schema(description = "Confirmación de contraseña", example = "SecurePass123!", required = true)
    private String confirmPassword;
}
