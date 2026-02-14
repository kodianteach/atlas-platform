package co.com.atlas.model.unit;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modelo de dominio para Unit (Unidad habitacional - Apartamento o Casa).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Unit {
    private Long id;
    private Long organizationId;
    private Long zoneId;
    private Long towerId;
    private String code;
    private UnitType type;
    private Integer floor;
    private BigDecimal areaSqm;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer parkingSpots;
    private Integer maxVehicles;
    
    /**
     * Indica si la unidad tiene habilitada la gestión de vehículos.
     * Si es false, vehicleLimit debe ser 0 o null.
     */
    private Boolean vehiclesEnabled;
    
    private UnitStatus status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
