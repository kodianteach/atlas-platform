package co.com.atlas.model.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Vehículo.
 * Un vehículo pertenece a una vivienda (Unit) dentro de una organización.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Vehicle {
    private Long id;
    private Long unitId;
    private Long organizationId;
    private String plate;
    private VehicleType vehicleType;
    private String brand;
    private String model;
    private String color;
    private String ownerName;
    private Boolean isActive;
    private Long registeredBy;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
