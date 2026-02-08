package co.com.atlas.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de autenticación con token JWT.
 * Toda la información del usuario está codificada dentro del token.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de autenticación exitosa con tokens JWT")
public class LoginResponse {
    
    @Schema(description = "Token de acceso JWT con toda la información del usuario codificada", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;
    
    @Schema(description = "Token de refresco para obtener nuevos tokens de acceso", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;
    
    @Schema(description = "Tipo de token", example = "Bearer")
    private String tokenType;
}
