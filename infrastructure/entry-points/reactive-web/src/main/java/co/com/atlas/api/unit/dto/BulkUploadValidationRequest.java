package co.com.atlas.api.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request para validación de carga masiva de unidades.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request para validación de carga masiva de unidades")
public class BulkUploadValidationRequest {

    @Schema(description = "Lista de filas a validar", required = true)
    private List<BulkRowDto> rows;

    /**
     * DTO para una fila del archivo de carga masiva.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "Fila individual para carga masiva")
    public static class BulkRowDto {
        
        @Schema(description = "Número de fila (1-based)", example = "1")
        private Integer rowNumber;
        
        @Schema(description = "Número de vivienda", example = "101")
        private String unitNumber;
        
        @Schema(description = "Prefijo/código de la unidad", example = "B")
        private String prefix;
        
        @Schema(description = "Email del propietario", example = "propietario@ejemplo.com")
        private String ownerEmail;
        
        @Schema(description = "Número de documento del propietario", example = "12345678")
        private String documentNumber;
        
        @Schema(description = "Tipo de documento", example = "CC", allowableValues = {"CC", "CE", "PASSPORT", "NIT"})
        private String documentType;
        
        @Schema(description = "Límite de vehículos permitidos", example = "2")
        private Integer vehicleLimit;
    }
}
