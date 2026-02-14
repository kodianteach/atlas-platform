package co.com.atlas.model.unit;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Resultado de carga masiva de unidades (Excel/CSV).
 * Contiene las filas válidas, errores y estado general.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class BulkUploadResult {
    
    /**
     * Filas válidas listas para procesar.
     */
    private List<BulkUnitRow> validRows;
    
    /**
     * Filas con errores.
     */
    private List<BulkUnitRow> errorRows;
    
    /**
     * Total de filas procesadas.
     */
    private Integer totalRows;
    
    /**
     * Total de filas válidas.
     */
    private Integer validCount;
    
    /**
     * Total de filas con errores.
     */
    private Integer errorCount;
    
    /**
     * Indica si hay errores críticos que impiden el procesamiento.
     */
    private Boolean hasCriticalErrors;
    
    /**
     * Mensaje descriptivo del resultado.
     */
    private String message;
    
    /**
     * Errores estructurales del archivo (columnas faltantes, formato, etc.)
     */
    private List<String> structuralErrors;
}
