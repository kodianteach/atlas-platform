package co.com.atlas.model.unit.validation;

import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.model.auth.validation.UserIdentificationValidator;
import co.com.atlas.model.unit.BulkUnitRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validador para filas de carga masiva de unidades.
 * Valida estructura y contenido del archivo Excel/CSV.
 * 
 * Estructura esperada:
 * - A: Número vivienda (obligatorio)
 * - B: Prefijo (obligatorio)
 * - C: Email propietario (obligatorio)
 * - D: Número identificación (obligatorio)
 * - E: Tipo identificación (obligatorio)
 * - F: Cantidad de vehículos (opcional)
 * 
 * Usa System.Logger en lugar de @Slf4j (regla de dominio).
 */
public final class BulkUploadValidator {
    
    private static final System.Logger LOGGER = System.getLogger(BulkUploadValidator.class.getName());
    
    private static final int MIN_REQUIRED_COLUMNS = 5;
    private static final String[] REQUIRED_COLUMN_NAMES = {
        "Número vivienda", "Prefijo", "Email propietario", 
        "Número identificación", "Tipo identificación"
    };
    
    private BulkUploadValidator() {
        // Utility class
    }
    
    /**
     * Valida la estructura del archivo (columnas mínimas requeridas).
     * 
     * @param columnCount número de columnas en el archivo
     * @return lista de errores estructurales (vacía si válido)
     */
    public static List<String> validateStructure(int columnCount) {
        List<String> errors = new ArrayList<>();
        
        if (columnCount < MIN_REQUIRED_COLUMNS) {
            errors.add("El archivo debe tener al menos " + MIN_REQUIRED_COLUMNS + 
                " columnas (A-E). Se encontraron: " + columnCount);
            
            StringBuilder sb = new StringBuilder("Columnas requeridas: ");
            for (int i = 0; i < REQUIRED_COLUMN_NAMES.length; i++) {
                sb.append((char)('A' + i)).append(" (").append(REQUIRED_COLUMN_NAMES[i]).append(")");
                if (i < REQUIRED_COLUMN_NAMES.length - 1) {
                    sb.append(", ");
                }
            }
            errors.add(sb.toString());
        }
        
        return errors;
    }
    
    /**
     * Valida una fila del archivo de carga masiva.
     * 
     * @param row fila a validar
     * @return la misma fila con errores y warnings poblados
     */
    public static BulkUnitRow validateRow(BulkUnitRow row) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Columna A: Número de vivienda (obligatorio)
        if (row.getUnitNumber() == null || row.getUnitNumber().isBlank()) {
            errors.add("Columna A (Número vivienda) es obligatoria");
        }
        
        // Columna B: Prefijo (obligatorio)
        if (row.getPrefix() == null || row.getPrefix().isBlank()) {
            errors.add("Columna B (Prefijo) es obligatoria");
        }
        
        // Columna C: Email propietario (obligatorio)
        if (row.getOwnerEmail() == null || row.getOwnerEmail().isBlank()) {
            errors.add("Columna C (Email propietario) es obligatoria");
        } else if (!isValidEmail(row.getOwnerEmail())) {
            errors.add("Columna C (Email) tiene formato inválido: " + row.getOwnerEmail());
        }
        
        // Columna D: Número identificación (obligatorio)
        if (row.getDocumentNumber() == null || row.getDocumentNumber().isBlank()) {
            errors.add("Columna D (Número identificación) es obligatoria");
        }
        
        // Columna E: Tipo identificación (obligatorio)
        if (row.getDocumentTypeCode() == null || row.getDocumentTypeCode().isBlank()) {
            errors.add("Columna E (Tipo identificación) es obligatoria");
        } else {
            DocumentType type = DocumentType.fromCode(row.getDocumentTypeCode());
            if (type == null) {
                errors.add("Columna E (Tipo identificación) no es válida: " + row.getDocumentTypeCode() + 
                    ". Valores permitidos: CC, NIT, CE, TI, PA, PEP");
            } else {
                row.setDocumentType(type);
                
                // Validar formato del documento
                if (row.getDocumentNumber() != null && !row.getDocumentNumber().isBlank()) {
                    if (!type.isValidFormat(row.getDocumentNumber())) {
                        errors.add("Columna D (Número identificación) tiene formato inválido para " + 
                            type.getDisplayName());
                    }
                }
            }
        }
        
        // Columna F: Cantidad de vehículos (opcional)
        if (row.getVehicleLimit() != null) {
            if (row.getVehicleLimit() < 0) {
                errors.add("Columna F (Cantidad de vehículos) no puede ser negativa");
            }
        }
        
        // Generar código de unidad
        if (row.getPrefix() != null && row.getUnitNumber() != null) {
            row.setGeneratedCode(row.generateCode());
        }
        
        // Establecer resultado de validación
        row.setErrors(errors);
        row.setWarnings(warnings);
        row.setValid(errors.isEmpty());
        
        return row;
    }
    
    /**
     * Valida duplicados en el lote de filas.
     * 
     * @param rows filas a validar
     * @return filas con errores de duplicación agregados
     */
    public static List<BulkUnitRow> validateDuplicatesInBatch(List<BulkUnitRow> rows) {
        Set<String> seenCodes = new HashSet<>();
        Set<String> seenDocuments = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        
        for (BulkUnitRow row : rows) {
            if (!row.getValid()) {
                continue; // Saltar filas ya inválidas
            }
            
            List<String> errors = new ArrayList<>(row.getErrors() != null ? row.getErrors() : List.of());
            
            // Verificar duplicado de código de unidad
            String code = row.getGeneratedCode();
            if (code != null) {
                if (seenCodes.contains(code)) {
                    errors.add("Código de unidad duplicado en el archivo: " + code);
                } else {
                    seenCodes.add(code);
                }
            }
            
            // Verificar duplicado de documento
            String docKey = row.getDocumentTypeCode() + "|" + row.getDocumentNumber();
            if (seenDocuments.contains(docKey)) {
                errors.add("Documento duplicado en el archivo: " + 
                    row.getDocumentTypeCode() + " " + row.getDocumentNumber());
            } else {
                seenDocuments.add(docKey);
            }
            
            // Verificar duplicado de email
            String email = row.getOwnerEmail() != null ? row.getOwnerEmail().toLowerCase().trim() : null;
            if (email != null) {
                if (seenEmails.contains(email)) {
                    errors.add("Email duplicado en el archivo: " + email);
                } else {
                    seenEmails.add(email);
                }
            }
            
            row.setErrors(errors);
            row.setValid(errors.isEmpty());
        }
        
        LOGGER.log(System.Logger.Level.INFO, 
            "Validación de duplicados completada: {0} códigos únicos, {1} documentos únicos, {2} emails únicos",
            seenCodes.size(), seenDocuments.size(), seenEmails.size());
        
        return rows;
    }
    
    /**
     * Validación básica de formato de email.
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
