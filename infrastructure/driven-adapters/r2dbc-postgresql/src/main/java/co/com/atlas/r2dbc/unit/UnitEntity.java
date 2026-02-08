package co.com.atlas.r2dbc.unit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad de base de datos para Unit.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("unit")
public class UnitEntity {
    
    @Id
    private Long id;
    
    @Column("organization_id")
    private Long organizationId;
    
    @Column("zone_id")
    private Long zoneId;
    
    @Column("tower_id")
    private Long towerId;
    
    private String code;
    
    private String type;
    
    private Integer floor;
    
    @Column("area_sqm")
    private BigDecimal areaSqm;
    
    private Integer bedrooms;
    
    private Integer bathrooms;
    
    @Column("parking_spots")
    private Integer parkingSpots;
    
    private String status;
    
    @Column("is_active")
    private Boolean isActive;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
    
    @Column("deleted_at")
    private Instant deletedAt;
}
