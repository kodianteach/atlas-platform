package co.com.atlas.api.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Respuesta de procesamiento de carga masiva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de procesamiento de carga masiva")
public class BulkUploadProcessResponse {
    
    @Schema(description = "Total de unidades creadas", example = "50")
    private int unitsCreated;
    
    @Schema(description = "Total de propietarios invitados", example = "30")
    private int ownersInvited;
    
    @Schema(description = "Total de errores durante el procesamiento", example = "2")
    private int errorsCount;
    
    @Schema(description = "IDs de las unidades creadas")
    private List<Long> unitIds;
    
    @Schema(description = "Errores durante procesamiento (código unidad -> mensaje)")
    private Map<String, String> errors;
    
    @Schema(description = "Mensaje descriptivo", example = "50 unidades creadas, 2 errores")
    private String message;
    
    @Schema(description = "¿Procesamiento exitoso?", example = "true")
    private boolean success;
}
