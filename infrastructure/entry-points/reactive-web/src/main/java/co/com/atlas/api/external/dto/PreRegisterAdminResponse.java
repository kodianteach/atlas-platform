package co.com.atlas.api.external.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response del pre-registro de administrador")
public class PreRegisterAdminResponse {
    
    @Schema(description = "ID del usuario creado", example = "123")
    private Long userId;
    
    @Schema(description = "ID del token de activación", example = "456")
    private Long tokenId;
    
    @Schema(description = "Email del usuario", example = "admin@conjunto.com")
    private String email;
    
    @Schema(description = "Nombre del usuario", example = "Juan Pérez")
    private String names;
    
    @Schema(description = "Tipo de documento", example = "CC")
    private String documentType;
    
    @Schema(description = "Número de documento (parcialmente oculto)", example = "****7890")
    private String documentNumberMasked;
    
    @Schema(description = "Fecha de expiración del token")
    private Instant expiresAt;
    
    @Schema(description = "Mensaje informativo", example = "Pre-registro exitoso. Se ha enviado un email.")
    private String message;
}
