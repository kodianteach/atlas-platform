package co.com.atlas.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para sincronización masiva de vehículos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSyncResponse {
    private Long unitId;
    private int created;
    private int updated;
    private int deleted;
    private List<VehicleResponse> vehicles;
    private String message;
}
