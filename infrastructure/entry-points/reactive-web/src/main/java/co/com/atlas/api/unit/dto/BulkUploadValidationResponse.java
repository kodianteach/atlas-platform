package co.com.atlas.api.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de validación de carga masiva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de validación de carga masiva")
public class BulkUploadValidationResponse {
    
    @Schema(description = "Total de filas válidas", example = "48")
    private int validRows;
    
    @Schema(description = "Total de filas con errores", example = "2")
    private int errorRows;
    
    @Schema(description = "Total de filas procesadas", example = "50")
    private int totalRows;
    
    @Schema(description = "¿Hay errores críticos que impiden procesar?", example = "false")
    private boolean hasCriticalErrors;
    
    @Schema(description = "Lista de errores por fila")
    private List<RowError> errors;
    
    @Schema(description = "Vista previa de las primeras filas válidas")
    private List<RowPreview> preview;
    
    @Schema(description = "Mensaje descriptivo", example = "48 filas válidas, 2 con errores")
    private String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Error en una fila del archivo")
    public static class RowError {
        @Schema(description = "Número de fila", example = "5")
        private int rowNumber;
        @Schema(description = "Campo con error", example = "documentNumber")
        private String field;
        @Schema(description = "Descripción del error", example = "Formato inválido para CC")
        private String error;
        @Schema(description = "Valor que causó el error", example = "ABC123")
        private String value;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Vista previa de una fila válida")
    public static class RowPreview {
        @Schema(description = "Número de fila", example = "1")
        private int rowNumber;
        @Schema(description = "Código de unidad", example = "APTO-101")
        private String unitCode;
        @Schema(description = "Tipo de unidad", example = "APARTMENT")
        private String unitType;
        @Schema(description = "Email del propietario", example = "propietario@email.com")
        private String ownerEmail;
        @Schema(description = "Tipo de documento del propietario", example = "CC")
        private String ownerDocumentType;
        @Schema(description = "Número de documento del propietario", example = "12345678")
        private String ownerDocumentNumber;
        @Schema(description = "Vehículos habilitados", example = "true")
        private Boolean vehiclesEnabled;
        @Schema(description = "Límite de vehículos", example = "2")
        private Integer vehicleLimit;
    }
}
