package co.com.atlas.api.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para completar el onboarding (crear company y organization).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteOnboardingRequest {
    
    /**
     * ID del usuario que completa el onboarding.
     */
    private Long userId;
    
    // ========== Datos de Company ==========
    
    /**
     * Nombre de la compañía (holding).
     */
    private String companyName;
    
    /**
     * NIT/RUT de la compañía (opcional).
     */
    private String companyTaxId;
    
    /**
     * Industria de la compañía (opcional).
     */
    private String companyIndustry;
    
    /**
     * Dirección de la compañía (opcional).
     */
    private String companyAddress;
    
    /**
     * País de la compañía (opcional).
     */
    private String companyCountry;
    
    /**
     * Ciudad de la compañía (opcional).
     */
    private String companyCity;
    
    // ========== Datos de Organization ==========
    
    /**
     * Nombre de la organización (ciudadela o conjunto).
     */
    private String organizationName;
    
    /**
     * Código de la organización (opcional, se genera si no se proporciona).
     */
    private String organizationCode;
    
    /**
     * Tipo de organización: CIUDADELA o CONJUNTO.
     */
    private String organizationType;
    
    /**
     * Si la organización usa zonas (default true).
     */
    private Boolean usesZones;
    
    /**
     * Descripción de la organización (opcional).
     */
    private String organizationDescription;
}
