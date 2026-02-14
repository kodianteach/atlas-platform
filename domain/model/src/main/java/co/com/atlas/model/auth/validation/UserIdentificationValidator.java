package co.com.atlas.model.auth.validation;

import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.model.common.BusinessException;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de identificación de usuario.
 * Valida tipo y número de documento según reglas colombianas.
 * 
 * Usa System.Logger en lugar de @Slf4j (regla de dominio).
 */
public final class UserIdentificationValidator {
    
    private static final System.Logger LOGGER = System.getLogger(UserIdentificationValidator.class.getName());
    
    private UserIdentificationValidator() {
        // Utility class
    }
    
    /**
     * Valida que el tipo de documento sea válido y exista en el sistema.
     * 
     * @param documentTypeCode código del tipo de documento
     * @throws BusinessException si el tipo no existe
     */
    public static void validateDocumentTypeExists(String documentTypeCode) {
        if (documentTypeCode == null || documentTypeCode.isBlank()) {
            throw new BusinessException("El tipo de documento es requerido", "DOCUMENT_TYPE_REQUIRED");
        }
        
        DocumentType type = DocumentType.fromCode(documentTypeCode);
        if (type == null) {
            LOGGER.log(System.Logger.Level.WARNING, "Tipo de documento no válido: {0}", documentTypeCode);
            throw new BusinessException(
                "Tipo de documento '" + documentTypeCode + "' no es válido. " +
                "Tipos permitidos: CC, NIT, CE, TI, PA, PEP",
                "INVALID_DOCUMENT_TYPE"
            );
        }
    }
    
    /**
     * Valida el formato del número de documento según su tipo.
     * 
     * @param documentType tipo de documento
     * @param documentNumber número de documento
     * @throws BusinessException si el formato es inválido
     */
    public static void validateDocumentFormat(DocumentType documentType, String documentNumber) {
        if (documentType == null) {
            throw new BusinessException("El tipo de documento es requerido", "DOCUMENT_TYPE_REQUIRED");
        }
        
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new BusinessException("El número de documento es requerido", "DOCUMENT_NUMBER_REQUIRED");
        }
        
        String normalized = documentType.normalize(documentNumber);
        
        if (!documentType.isValidFormat(normalized)) {
            LOGGER.log(System.Logger.Level.WARNING, 
                "Formato de documento inválido: tipo={0}, número={1}", 
                documentType.name(), documentNumber);
            throw new BusinessException(
                "Formato de número de documento inválido para " + documentType.getDisplayName() + ". " +
                "Formato esperado: " + documentType.getValidationRegex() + " " +
                "(longitud: " + documentType.getMinLength() + "-" + documentType.getMaxLength() + " caracteres)",
                "INVALID_DOCUMENT_FORMAT"
            );
        }
    }
    
    /**
     * Valida que ambos campos de identificación estén presentes.
     * 
     * @param documentTypeCode código del tipo de documento
     * @param documentNumber número de documento
     * @throws BusinessException si falta algún campo
     */
    public static void validateBothFieldsPresent(String documentTypeCode, String documentNumber) {
        List<String> missingFields = new ArrayList<>();
        
        if (documentTypeCode == null || documentTypeCode.isBlank()) {
            missingFields.add("tipo de documento");
        }
        
        if (documentNumber == null || documentNumber.isBlank()) {
            missingFields.add("número de documento");
        }
        
        if (!missingFields.isEmpty()) {
            throw new BusinessException(
                "Campos de identificación requeridos: " + String.join(", ", missingFields),
                "IDENTIFICATION_FIELDS_REQUIRED"
            );
        }
    }
    
    /**
     * Realiza validación completa de identificación de usuario.
     * 
     * @param documentTypeCode código del tipo de documento
     * @param documentNumber número de documento
     * @return el tipo de documento parseado
     * @throws BusinessException si hay errores de validación
     */
    public static DocumentType validateComplete(String documentTypeCode, String documentNumber) {
        validateBothFieldsPresent(documentTypeCode, documentNumber);
        validateDocumentTypeExists(documentTypeCode);
        
        DocumentType type = DocumentType.fromCode(documentTypeCode);
        validateDocumentFormat(type, documentNumber);
        
        LOGGER.log(System.Logger.Level.DEBUG, 
            "Validación de documento exitosa: tipo={0}, número=****{1}", 
            type.name(), 
            documentNumber.length() > 4 ? documentNumber.substring(documentNumber.length() - 4) : "****");
        
        return type;
    }
    
    /**
     * Normaliza un número de documento según su tipo.
     * 
     * @param documentType tipo de documento
     * @param documentNumber número de documento
     * @return número normalizado
     */
    public static String normalizeDocumentNumber(DocumentType documentType, String documentNumber) {
        if (documentType == null || documentNumber == null) {
            return documentNumber;
        }
        return documentType.normalize(documentNumber);
    }
}
