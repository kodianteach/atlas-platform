package co.com.atlas.r2dbc.tower;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para Tower.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("tower")
public class TowerEntity {
    
    @Id
    private Long id;
    
    @Column("zone_id")
    private Long zoneId;
    
    private String code;
    
    private String name;
    
    @Column("floors_count")
    private Integer floorsCount;
    
    private String description;
    
    @Column("sort_order")
    private Integer sortOrder;
    
    @Column("is_active")
    private Boolean isActive;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
    
    @Column("deleted_at")
    private Instant deletedAt;
}
