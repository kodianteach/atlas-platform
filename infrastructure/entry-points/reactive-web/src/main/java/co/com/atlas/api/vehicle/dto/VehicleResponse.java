package co.com.atlas.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para un vehículo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private Long id;
    private Long unitId;
    private Long organizationId;
    private String plate;
    private String vehicleType;
    private String brand;
    private String model;
    private String color;
    private String ownerName;
    private Boolean isActive;
    private Long registeredBy;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
