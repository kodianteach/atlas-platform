package co.com.atlas.api.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Información del propietario para creación/invitación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del propietario para creación/invitación")
public class OwnerInfoRequest {
    
    @Schema(description = "Email del propietario", example = "propietario@email.com", required = true)
    private String email;
    
    @Schema(description = "Tipo de documento", example = "CC", allowableValues = {"CC", "NIT", "CE", "TI", "PA", "PEP"})
    private String documentType;
    
    @Schema(description = "Número de documento", example = "12345678")
    private String documentNumber;
    
    @Schema(description = "Nombres completos del propietario", example = "Juan Pérez")
    private String names;
    
    @Schema(description = "Teléfono de contacto", example = "+573001234567")
    private String phone;
}
