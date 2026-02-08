package co.com.atlas.api.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para pre-registro de administrador externo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreRegisterAdminRequest {
    
    /**
     * Email del administrador a pre-registrar.
     */
    private String email;
    
    /**
     * Nombre completo del administrador.
     */
    private String names;
    
    /**
     * Teléfono (opcional).
     */
    private String phone;
    
    /**
     * Horas de expiración del token (opcional, default 24h, min 1h, max 168h).
     */
    private Integer expirationHours;
    
    /**
     * URL base de activación (donde se redirigirá con el token).
     * Ejemplo: "https://app.atlasplatform.com/activate"
     */
    private String activationBaseUrl;
    
    /**
     * Metadata adicional en formato JSON (opcional).
     * Puede incluir: companyNameSuggestion, organizationNameSuggestion, etc.
     */
    private String metadata;
}
