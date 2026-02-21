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
 * Entidad de base de datos para porter_enrollment_audit_log.
 * Sigue el patrón de preregistration_audit_log.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("porter_enrollment_audit_log")
public class PorterEnrollmentAuditEntity {

    @Id
    private Long id;

    @Column("token_id")
    private Long tokenId;

    private String action;

    @Column("performed_by")
    private Long performedBy;

    @Column("ip_address")
    private String ipAddress;

    @Column("user_agent")
    private String userAgent;

    private String details;

    @Column("created_at")
    private Instant createdAt;
}
