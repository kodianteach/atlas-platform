package co.com.atlas.api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de error de la API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de error de la API")
public class ErrorResponse {
    
    @Schema(description = "Código de error", example = "AUTH_001")
    private String code;
    
    @Schema(description = "Mensaje descriptivo del error", example = "Credenciales inválidas")
    private String message;
    
    @Schema(description = "Fecha y hora del error", example = "2026-01-06T10:30:00Z")
    private String timestamp;
    
    @Schema(description = "Ruta del endpoint que generó el error", example = "/api/auth/login")
    private String path;
}
