package co.com.atlas.r2dbc.userrolemulti;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para UserRoleMulti.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("user_roles_multi")
public class UserRoleMultiEntity {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("organization_id")
    private Long organizationId;
    
    @Column("role_id")
    private Long roleId;
    
    @Column("is_primary")
    private Boolean isPrimary;
    
    @Column("assigned_at")
    private Instant assignedAt;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
