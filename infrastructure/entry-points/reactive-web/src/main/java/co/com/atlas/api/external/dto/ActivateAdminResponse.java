package co.com.atlas.api.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response de la activación de administrador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivateAdminResponse {
    
    /**
     * ID del usuario activado.
     */
    private Long userId;
    
    /**
     * Email del usuario.
     */
    private String email;
    
    /**
     * Nombre del usuario.
     */
    private String names;
    
    /**
     * Estado actual del usuario.
     */
    private String status;
    
    /**
     * Mensaje informativo.
     */
    private String message;
}
