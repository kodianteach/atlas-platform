package co.com.atlas.model.common;

/**
 * Excepción para recursos duplicados.
 */
public class DuplicateException extends BusinessException {
    
    public DuplicateException(String message) {
        super(message, "DUPLICATE", 409);
    }
    
    public DuplicateException(String resource, String field, String value) {
        super(resource + " con " + field + " '" + value + "' ya existe", "DUPLICATE", 409);
    }
}
