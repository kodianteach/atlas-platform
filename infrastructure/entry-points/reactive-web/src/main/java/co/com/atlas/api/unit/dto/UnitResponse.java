package co.com.atlas.api.unit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de respuesta para Unit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitResponse {
    private Long id;
    private Long organizationId;
    private Long zoneId;
    private Long towerId;
    private String code;
    private String type;
    private Integer floor;
    private BigDecimal areaSqm;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer parkingSpots;
    private Integer maxVehicles;
    private String status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
