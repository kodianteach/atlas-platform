package co.com.atlas.model.unit;

import co.com.atlas.model.auth.DocumentType;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Información del propietario para asignar a una unidad.
 * Usado en flujos de distribución y carga masiva.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class OwnerInfo {
    
    /**
     * Email del propietario.
     * Requerido.
     */
    private String email;
    
    /**
     * Tipo de documento de identificación.
     * Requerido.
     */
    private DocumentType documentType;
    
    /**
     * Número de documento de identificación.
     * Requerido.
     */
    private String documentNumber;
    
    /**
     * Nombre completo del propietario.
     * Opcional en distribución, puede obtenerse del usuario existente.
     */
    private String names;
    
    /**
     * Teléfono del propietario.
     * Opcional.
     */
    private String phone;
    
    /**
     * Indica si el propietario ya existe en el sistema.
     * Usado internamente durante el procesamiento.
     */
    private Boolean existingUser;
    
    /**
     * ID del usuario existente (si aplica).
     * Usado internamente durante el procesamiento.
     */
    private Long userId;
}
