package co.com.atlas.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar un vehículo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequest {
    private Long unitId;
    private String plate;
    private String vehicleType;
    private String brand;
    private String model;
    private String color;
    private String ownerName;
    private Boolean isActive;
    private String notes;
}
