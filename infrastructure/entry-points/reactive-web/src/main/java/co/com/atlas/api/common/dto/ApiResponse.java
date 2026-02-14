package co.com.atlas.api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Respuesta estándar de la API.
 * Encapsula todas las respuestas con un formato consistente.
 *
 * @param <T> Tipo de datos de la respuesta
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estándar de la API")
public class ApiResponse<T> {
    
    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private boolean success;
    
    @Schema(description = "Código de estado HTTP", example = "200")
    private int status;
    
    @Schema(description = "Mensaje descriptivo de la respuesta", example = "Operación exitosa")
    private String message;
    
    @Schema(description = "Código de error específico", example = "ERR_001")
    private String errorCode;
    
    @Schema(description = "Datos de la respuesta")
    private T data;
    
    @Schema(description = "Información adicional o metadatos")
    private Object metadata;
    
    @Schema(description = "Timestamp de la respuesta", example = "2026-01-08T10:30:00Z")
    @Builder.Default
    private String timestamp = Instant.now().toString();
    
    @Schema(description = "Ruta del endpoint", example = "/api/auth/login")
    private String path;
    
    /**
     * Crea una respuesta exitosa con datos.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * Crea una respuesta exitosa sin datos.
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .build();
    }
    
    /**
     * Crea una respuesta de error.
     */
    public static <T> ApiResponse<T> error(int status, String message, String path, Map<String, Object> metadata) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .path(path)
                .metadata(metadata)
                .build();
    }
    
    /**
     * Crea una respuesta de error simple.
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .build();
    }
    
    /**
     * Crea una respuesta de error con mensaje (usa status 400 por defecto).
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(400)
                .message(message)
                .build();
    }
}
