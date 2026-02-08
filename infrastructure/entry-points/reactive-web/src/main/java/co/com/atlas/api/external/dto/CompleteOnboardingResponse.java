package co.com.atlas.api.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response del onboarding completado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteOnboardingResponse {
    
    /**
     * ID de la compañía creada.
     */
    private Long companyId;
    
    /**
     * Slug de la compañía (para URLs).
     */
    private String companySlug;
    
    /**
     * ID de la organización creada.
     */
    private Long organizationId;
    
    /**
     * Código de la organización.
     */
    private String organizationCode;
    
    /**
     * Mensaje informativo.
     */
    private String message;
}
