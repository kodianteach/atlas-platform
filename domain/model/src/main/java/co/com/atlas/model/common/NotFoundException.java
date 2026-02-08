package co.com.atlas.model.common;

/**
 * Excepción para recursos no encontrados.
 */
public class NotFoundException extends BusinessException {
    
    public NotFoundException(String resource) {
        super(resource + " no encontrado", "NOT_FOUND", 404);
    }
    
    public NotFoundException(String resource, Long id) {
        super(resource + " con ID " + id + " no encontrado", "NOT_FOUND", 404);
    }
}
