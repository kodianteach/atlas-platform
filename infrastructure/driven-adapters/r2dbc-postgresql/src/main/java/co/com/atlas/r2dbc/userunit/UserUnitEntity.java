package co.com.atlas.r2dbc.userunit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidad de base de datos para UserUnit.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("user_units")
public class UserUnitEntity {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("unit_id")
    private Long unitId;
    
    @Column("role_id")
    private Long roleId;
    
    @Column("ownership_type")
    private String ownershipType;
    
    @Column("is_primary")
    private Boolean isPrimary;
    
    @Column("move_in_date")
    private LocalDate moveInDate;
    
    @Column("status")
    private String status;
    
    @Column("invited_by")
    private Long invitedBy;
    
    @Column("joined_at")
    private Instant joinedAt;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
    
    @Column("deleted_at")
    private Instant deletedAt;
}
