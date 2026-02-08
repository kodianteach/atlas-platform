package co.com.atlas.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credenciales de autenticación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Credenciales de autenticación del usuario")
public class LoginRequest {
    
    @Schema(description = "Correo electrónico del usuario", example = "admin@atlas.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    
    @Schema(description = "Contraseña del usuario", example = "Admin123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
