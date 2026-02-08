package co.com.atlas.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request para verificar un token JWT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request para verificar el contenido de un token JWT")
public class VerifyTokenRequest {
    
    @Schema(description = "Token JWT a verificar", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;
}
