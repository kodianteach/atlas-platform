package co.com.atlas.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud para refrescar el token de acceso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para renovar el token de acceso")
public class RefreshTokenRequest {
    
    @Schema(description = "Token de refresco válido", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
