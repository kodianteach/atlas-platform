package co.com.atlas.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para inactivación masiva de vehículos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkInactivateResponse {
    private Long unitId;
    private int inactivatedCount;
    private String message;
}
