package co.com.atlas.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de request para sincronización masiva de vehículos de una unidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSyncRequest {
    /** Lista completa deseada de vehículos para la unidad. */
    private List<VehicleRequest> vehicles;
}
