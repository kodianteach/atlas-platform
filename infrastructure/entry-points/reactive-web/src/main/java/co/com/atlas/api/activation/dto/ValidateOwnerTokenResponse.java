package co.com.atlas.api.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de validación de token de propietario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de validación de token de propietario")
public class ValidateOwnerTokenResponse {
    
    @Schema(description = "¿Token válido?", example = "true")
    private boolean valid;
    
    @Schema(description = "Email del propietario invitado", example = "propietario@email.com")
    private String email;
    
    @Schema(description = "Nombre del propietario", example = "Juan Pérez")
    private String names;
    
    @Schema(description = "Nombre de la organización", example = "Torres del Parque")
    private String organizationName;
    
    @Schema(description = "Código de la unidad asignada", example = "APTO-101")
    private String unitCode;
    
    @Schema(description = "ID de la invitación", example = "123")
    private Long invitationId;
    
    @Schema(description = "¿Usuario ya existe?", example = "false")
    private boolean userExists;
    
    @Schema(description = "Mensaje descriptivo", example = "Token válido")
    private String message;
    
    @Schema(description = "Código de error (si no es válido)", example = "TOKEN_EXPIRED")
    private String errorCode;
}
