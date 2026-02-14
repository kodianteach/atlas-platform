package co.com.atlas.api.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Respuesta de distribución de unidades.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de distribución de unidades")
public class UnitDistributionResponse {
    
    @Schema(description = "Total de unidades creadas", example = "10")
    private int unitsCreated;
    
    @Schema(description = "IDs de las unidades creadas")
    private List<Long> unitIds;
    
    @Schema(description = "Códigos de las unidades creadas")
    private List<String> unitCodes;
    
    @Schema(description = "Invitaciones enviadas", example = "1")
    private int invitationsSent;
    
    @Schema(description = "Mensaje descriptivo", example = "10 unidades creadas exitosamente")
    private String message;
    
    @Schema(description = "Errores por unidad (código -> mensaje de error)")
    private Map<String, String> errors;
}
