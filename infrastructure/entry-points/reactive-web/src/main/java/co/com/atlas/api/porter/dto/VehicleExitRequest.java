package co.com.atlas.api.porter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para registro de salida de vehículo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleExitRequest {
    private String vehiclePlate;
    private String personName;
}
