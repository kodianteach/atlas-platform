package co.com.atlas.model.unit;

import java.util.Collections;
import java.util.List;

/**
 * Value object que encapsula el resultado de una distribución masiva de unidades.
 * Contiene listas separadas de unidades creadas exitosamente y unidades rechazadas.
 *
 * @param createdUnits  Lista de unidades creadas exitosamente
 * @param rejectedUnits Lista de unidades rechazadas con motivo
 */
public record UnitDistributionResult(
        List<Unit> createdUnits,
        List<RejectedUnit> rejectedUnits
) {

    /**
     * Constructor compacto que garantiza listas inmutables no nulas.
     */
    public UnitDistributionResult {
        createdUnits = createdUnits != null ? List.copyOf(createdUnits) : Collections.emptyList();
        rejectedUnits = rejectedUnits != null ? List.copyOf(rejectedUnits) : Collections.emptyList();
    }

    /**
     * @return cantidad de unidades creadas exitosamente
     */
    public int getCreatedCount() {
        return createdUnits.size();
    }

    /**
     * @return cantidad de unidades rechazadas
     */
    public int getRejectedCount() {
        return rejectedUnits.size();
    }

    /**
     * @return true si todas las unidades fueron creadas (ninguna rechazada)
     */
    public boolean isFullSuccess() {
        return rejectedUnits.isEmpty() && !createdUnits.isEmpty();
    }

    /**
     * @return true si ninguna unidad fue creada (todas rechazadas)
     */
    public boolean isFullRejection() {
        return createdUnits.isEmpty() && !rejectedUnits.isEmpty();
    }

    /**
     * @return true si hubo creación parcial (algunas creadas, algunas rechazadas)
     */
    public boolean isPartial() {
        return !createdUnits.isEmpty() && !rejectedUnits.isEmpty();
    }
}
