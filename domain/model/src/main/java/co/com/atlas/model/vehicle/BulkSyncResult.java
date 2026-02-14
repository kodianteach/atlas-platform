package co.com.atlas.model.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Resultado de la operación de sincronización masiva de vehículos para una unidad.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class BulkSyncResult {
    /** ID de la unidad sincronizada. */
    private Long unitId;
    /** Vehículos creados durante la sincronización. */
    private int created;
    /** Vehículos actualizados durante la sincronización. */
    private int updated;
    /** Vehículos eliminados (soft delete) durante la sincronización. */
    private int deleted;
    /** Lista final de vehículos de la unidad tras la sincronización. */
    private List<Vehicle> vehicles;
    /** Mensaje descriptivo. */
    private String message;
}
