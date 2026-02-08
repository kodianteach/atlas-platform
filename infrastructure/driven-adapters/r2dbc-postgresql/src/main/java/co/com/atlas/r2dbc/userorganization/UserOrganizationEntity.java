package co.com.atlas.r2dbc.userorganization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para UserOrganization.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("user_organizations")
public class UserOrganizationEntity {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("organization_id")
    private Long organizationId;
    
    private String status;
    
    @Column("joined_at")
    private Instant joinedAt;
    
    @Column("left_at")
    private Instant leftAt;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
