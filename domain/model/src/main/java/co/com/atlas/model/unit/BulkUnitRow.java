package co.com.atlas.model.unit;

import co.com.atlas.model.auth.DocumentType;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Representa una fila del archivo de carga masiva de unidades.
 * Estructura:
 * - A: Número vivienda
 * - B: Prefijo
 * - C: Email propietario
 * - D: Número identificación
 * - E: Tipo identificación
 * - F: Cantidad de vehículos (OPCIONAL)
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class BulkUnitRow {
    
    /**
     * Número de fila en el archivo (1-based).
     */
    private Integer rowNumber;
    
    /**
     * Columna A: Número de vivienda.
     */
    private String unitNumber;
    
    /**
     * Columna B: Prefijo/código de la unidad.
     */
    private String prefix;
    
    /**
     * Columna C: Email del propietario.
     */
    private String ownerEmail;
    
    /**
     * Columna D: Número de documento de identificación.
     */
    private String documentNumber;
    
    /**
     * Columna E: Tipo de documento de identificación.
     */
    private String documentTypeCode;
    
    /**
     * Tipo de documento parseado.
     */
    private DocumentType documentType;
    
    /**
     * Columna F: Cantidad de vehículos permitidos (opcional).
     */
    private Integer vehicleLimit;
    
    /**
     * Código generado de la unidad (prefix + unitNumber).
     */
    private String generatedCode;
    
    /**
     * Indica si la fila es válida.
     */
    private Boolean valid;
    
    /**
     * Errores encontrados en la fila.
     */
    private List<String> errors;
    
    /**
     * Advertencias para la fila.
     */
    private List<String> warnings;
    
    /**
     * Genera el código de la unidad.
     * @return código en formato PREFIX-NUMBER
     */
    public String generateCode() {
        if (prefix == null || unitNumber == null) {
            return null;
        }
        return prefix.trim() + "-" + unitNumber.trim();
    }
    
    /**
     * Indica si la columna F (vehículos) fue proporcionada.
     */
    public boolean hasVehicleLimit() {
        return vehicleLimit != null;
    }
}
