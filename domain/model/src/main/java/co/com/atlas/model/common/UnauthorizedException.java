package co.com.atlas.model.common;

/**
 * Excepción para acceso no autorizado.
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", 403);
    }
}
