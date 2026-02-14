package co.com.atlas.model.auth;

import java.util.regex.Pattern;

/**
 * Tipos de documento de identificación soportados.
 * Incluye patrones de validación según estándares colombianos.
 */
public enum DocumentType {
    /**
     * Cédula de Ciudadanía - Colombia
     * Para mayores de edad colombianos.
     */
    CC("Cédula de Ciudadanía", "^[0-9]{6,10}$", 6, 10, DocumentAppliesTo.PERSON),

    /**
     * Número de Identificación Tributaria
     * Para empresas y personas jurídicas.
     */
    NIT("Número de Identificación Tributaria", "^[0-9]{9,10}(-[0-9])?$", 9, 11, DocumentAppliesTo.COMPANY),

    /**
     * Cédula de Extranjería
     * Para extranjeros residentes en Colombia.
     */
    CE("Cédula de Extranjería", "^[0-9]{6,7}$", 6, 7, DocumentAppliesTo.PERSON),

    /**
     * Tarjeta de Identidad
     * Para menores de edad colombianos.
     */
    TI("Tarjeta de Identidad", "^[0-9]{10,11}$", 10, 11, DocumentAppliesTo.PERSON),

    /**
     * Pasaporte
     * Documento de viaje internacional.
     */
    PA("Pasaporte", "^[A-Z0-9]{5,20}$", 5, 20, DocumentAppliesTo.PERSON),

    /**
     * Permiso Especial de Permanencia
     * Para migrantes venezolanos.
     */
    PEP("Permiso Especial de Permanencia", "^[0-9]{15}$", 15, 15, DocumentAppliesTo.PERSON);

    private final String displayName;
    private final String validationRegex;
    private final int minLength;
    private final int maxLength;
    private final DocumentAppliesTo appliesTo;
    private final Pattern compiledPattern;

    DocumentType(String displayName, String validationRegex, int minLength, int maxLength, DocumentAppliesTo appliesTo) {
        this.displayName = displayName;
        this.validationRegex = validationRegex;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.appliesTo = appliesTo;
        this.compiledPattern = Pattern.compile(validationRegex);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getValidationRegex() {
        return validationRegex;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public DocumentAppliesTo getAppliesTo() {
        return appliesTo;
    }

    /**
     * Valida si el número de documento cumple con el formato esperado para este tipo.
     *
     * @param documentNumber el número de documento a validar
     * @return true si el formato es válido
     */
    public boolean isValidFormat(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return false;
        }
        String normalized = documentNumber.trim().toUpperCase();
        return compiledPattern.matcher(normalized).matches();
    }

    /**
     * Normaliza el número de documento (mayúsculas, sin espacios).
     *
     * @param documentNumber el número de documento a normalizar
     * @return documento normalizado
     */
    public String normalize(String documentNumber) {
        if (documentNumber == null) {
            return null;
        }
        return documentNumber.trim().toUpperCase().replaceAll("\\s+", "");
    }

    /**
     * Obtiene el tipo de documento desde su código.
     *
     * @param code el código del tipo de documento
     * @return el tipo de documento o null si no existe
     */
    public static DocumentType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DocumentType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Verifica si un código de documento es válido.
     *
     * @param code el código a verificar
     * @return true si el código existe
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }

    /**
     * Indica a qué tipo de entidad aplica el documento.
     */
    public enum DocumentAppliesTo {
        PERSON,
        COMPANY,
        BOTH
    }
}
