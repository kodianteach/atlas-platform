package co.com.atlas.api.config;

import co.com.atlas.model.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manejador global de excepciones para WebFlux.
 * Usa WebExceptionHandler con máxima prioridad para capturar TODAS las excepciones.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Throwable rootCause = getRootCause(ex);
        
        log.error("Error en [{}]: {} - {}", exchange.getRequest().getPath(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        HttpStatus status;
        String errorCode;
        String message;
        
        // Verificar BusinessException (original o causa raíz)
        if (ex instanceof BusinessException be) {
            status = HttpStatus.resolve(be.getHttpStatus());
            if (status == null) status = HttpStatus.BAD_REQUEST;
            errorCode = be.getErrorCode();
            message = be.getMessage();
        } else if (rootCause instanceof BusinessException be) {
            status = HttpStatus.resolve(be.getHttpStatus());
            if (status == null) status = HttpStatus.BAD_REQUEST;
            errorCode = be.getErrorCode();
            message = be.getMessage();
        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "INVALID_ARGUMENT";
            message = ex.getMessage();
        } else if (ex instanceof IllegalStateException) {
            status = HttpStatus.CONFLICT;
            errorCode = "INVALID_STATE";
            message = ex.getMessage();
        } else if (isDataIntegrityViolation(ex, rootCause)) {
            status = HttpStatus.CONFLICT;
            String rootMessage = rootCause.getMessage() != null ? rootCause.getMessage() : ex.getMessage();
            Map<String, String> duplicateInfo = parseDuplicateKeyError(rootMessage);
            if (duplicateInfo != null) {
                errorCode = "DUPLICATE_ENTRY";
                message = duplicateInfo.get("message");
            } else {
                errorCode = "DATA_INTEGRITY_ERROR";
                message = "Error de integridad de datos. Por favor verifique los datos ingresados.";
            }
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_ERROR";
            message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = rootCause.getMessage();
            }
            if (message == null || message.isBlank()) {
                message = ex.getClass().getSimpleName();
            }
        }
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("path", exchange.getRequest().getPath().value());
        body.put("status", status.value());
        body.put("success", false);
        body.put("message", message);
        body.put("errorCode", errorCode);
        body.put("errorType", ex.getClass().getSimpleName());
        
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] fallback = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(fallback);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
    
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
    
    /**
     * Verifica si la excepción es una violación de integridad de datos (R2DBC).
     */
    private boolean isDataIntegrityViolation(Throwable ex, Throwable rootCause) {
        String exClassName = ex.getClass().getName();
        String rootClassName = rootCause.getClass().getName();
        return exClassName.contains("DataIntegrityViolation") ||
               exClassName.contains("R2dbcDataIntegrityViolationException") ||
               rootClassName.contains("DataIntegrityViolation") ||
               rootClassName.contains("R2dbcDataIntegrityViolationException");
    }
    
    /**
     * Parsea errores de llave duplicada de MySQL y retorna un mensaje amigable.
     * Ejemplo de entrada: "Duplicate entry 'CC-1234567890' for key 'users.idx_users_document_unique'"
     */
    private Map<String, String> parseDuplicateKeyError(String errorMessage) {
        if (errorMessage == null || !errorMessage.contains("Duplicate entry")) {
            return null;
        }
        
        // Extraer el valor duplicado y la llave
        Pattern pattern = Pattern.compile("Duplicate entry '([^']+)' for key '([^']+)'");
        Matcher matcher = pattern.matcher(errorMessage);
        
        if (matcher.find()) {
            String duplicateValue = matcher.group(1);
            String keyName = matcher.group(2);
            
            String friendlyMessage;
            
            // Mapear nombres de índices a mensajes amigables
            if (keyName.contains("document") || keyName.contains("Document")) {
                friendlyMessage = "Ya existe un usuario registrado con el documento: " + duplicateValue;
            } else if (keyName.contains("email") || keyName.contains("Email")) {
                friendlyMessage = "Ya existe un usuario registrado con el email: " + duplicateValue;
            } else if (keyName.contains("plate") || keyName.contains("Plate")) {
                friendlyMessage = "Ya existe un vehículo registrado con la placa: " + duplicateValue;
            } else if (keyName.contains("code") || keyName.contains("Code")) {
                friendlyMessage = "Ya existe un registro con el código: " + duplicateValue;
            } else {
                friendlyMessage = "Ya existe un registro con el valor: " + duplicateValue;
            }
            
            Map<String, String> result = new LinkedHashMap<>();
            result.put("message", friendlyMessage);
            result.put("value", duplicateValue);
            result.put("key", keyName);
            return result;
        }
        
        return null;
    }
}


