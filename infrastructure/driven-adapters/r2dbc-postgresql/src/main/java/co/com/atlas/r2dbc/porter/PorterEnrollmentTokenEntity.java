package co.com.atlas.r2dbc.porter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para porter_enrollment_tokens.
 * Sigue el patrón de admin_activation_tokens (V5).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("porter_enrollment_tokens")
public class PorterEnrollmentTokenEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("organization_id")
    private Long organizationId;

    @Column("token_hash")
    private String tokenHash;

    private String status;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("consumed_at")
    private Instant consumedAt;

    @Column("created_by")
    private Long createdBy;

    @Column("ip_address")
    private String ipAddress;

    @Column("user_agent")
    private String userAgent;

    @Column("activation_ip")
    private String activationIp;

    @Column("activation_user_agent")
    private String activationUserAgent;

    private String metadata;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
