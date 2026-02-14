package co.com.atlas.api.external.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request para pre-registro de administrador externo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para pre-registro de administrador externo")
public class PreRegisterAdminRequest {
    
    @Schema(description = "Email del administrador a pre-registrar", example = "admin@conjunto.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    
    @Schema(description = "Nombre completo del administrador", example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
    private String names;
    
    @Schema(description = "Tipo de documento de identidad", example = "CC", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"CC", "CE", "NIT", "PA", "TI", "PEP"})
    private String documentType;
    
    @Schema(description = "Número de documento de identidad", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    private String documentNumber;
    
    @Schema(description = "Teléfono del administrador (opcional)", example = "+57 300 123 4567")
    private String phone;
    
    @Schema(description = "Horas de expiración del token (opcional, default 72h, min 1h, max 168h)", example = "72", minimum = "1", maximum = "168")
    private Integer expirationHours;
    
    @Schema(description = "URL base de activación donde se redirigirá con el token", example = "https://app.atlasplatform.com/activate", requiredMode = Schema.RequiredMode.REQUIRED)
    private String activationBaseUrl;
    
    @Schema(description = "Metadata adicional (opcional). Puede incluir: companyNameSuggestion, organizationNameSuggestion, etc.")
    private Map<String, Object> metadata;
}
