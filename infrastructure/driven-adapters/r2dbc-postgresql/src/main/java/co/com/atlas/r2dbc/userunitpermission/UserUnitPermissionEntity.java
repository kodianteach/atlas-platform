package co.com.atlas.r2dbc.userunitpermission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para user_unit_permissions.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("user_unit_permissions")
public class UserUnitPermissionEntity {

    @Id
    private Long id;

    @Column("user_unit_id")
    private Long userUnitId;

    @Column("permission_id")
    private Long permissionId;

    @Column("granted_by")
    private Long grantedBy;

    @Column("granted_at")
    private Instant grantedAt;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("created_at")
    private Instant createdAt;
}
