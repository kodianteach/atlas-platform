package co.com.atlas.api.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de activación completa de propietario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de activación completa de propietario")
public class CompleteOwnerActivationResponse {
    
    @Schema(description = "ID del usuario activado", example = "123")
    private Long userId;
    
    @Schema(description = "Email del usuario", example = "propietario@email.com")
    private String email;
    
    @Schema(description = "Nombres del propietario", example = "Juan Pérez")
    private String names;
    
    @Schema(description = "ID de la organización", example = "1")
    private Long organizationId;
    
    @Schema(description = "Nombre de la organización", example = "Torres del Parque")
    private String organizationName;
    
    @Schema(description = "Código de la unidad", example = "APTO-101")
    private String unitCode;
    
    @Schema(description = "Mensaje de éxito", example = "Cuenta activada exitosamente")
    private String message;
    
    @Schema(description = "¿Activación exitosa?", example = "true")
    private boolean success;
}
