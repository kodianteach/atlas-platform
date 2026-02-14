package co.com.atlas.model.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Resultado de la validación de placa para guardas.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class PlateValidationResult {
    /** Indica si la placa está registrada y activa. */
    private boolean allowed;
    /** Placa consultada. */
    private String plate;
    /** Código de la vivienda (unidad). */
    private String unitCode;
    /** Tipo de vehículo. */
    private String vehicleType;
    /** Nombre del propietario registrado. */
    private String ownerName;
    /** Mensaje descriptivo. */
    private String message;
}
