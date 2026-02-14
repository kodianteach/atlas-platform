package co.com.atlas.model.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Resultado de la operación bulk de inactivar vehículos de una unidad.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class BulkInactivateResult {
    /** ID de la unidad afectada. */
    private Long unitId;
    /** Cantidad de vehículos inactivados. */
    private int inactivatedCount;
    /** Mensaje descriptivo. */
    private String message;
}
