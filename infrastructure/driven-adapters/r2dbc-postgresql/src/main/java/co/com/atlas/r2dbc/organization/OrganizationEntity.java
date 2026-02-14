package co.com.atlas.r2dbc.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para Organization.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("organization")
public class OrganizationEntity {
    
    @Id
    private Long id;
    
    @Column("company_id")
    private Long companyId;
    
    private String code;
    
    private String name;
    
    private String slug;
    
    private String type;
    
    @Column("uses_zones")
    private Boolean usesZones;
    
    @Column("allowed_unit_types")
    private String allowedUnitTypes;
    
    private String description;
    
    private String settings;
    
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
