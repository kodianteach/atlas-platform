package co.com.atlas.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Response con el contenido decodificado del token JWT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Contenido decodificado del token JWT")
public class VerifyTokenResponse {
    
    @Schema(description = "Claims del token decodificado")
    private Map<String, Object> claims;
}
