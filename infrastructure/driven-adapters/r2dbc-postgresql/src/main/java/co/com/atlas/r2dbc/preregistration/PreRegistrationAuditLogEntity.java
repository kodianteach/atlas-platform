package co.com.atlas.r2dbc.preregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad R2DBC para preregistration_audit_log.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("preregistration_audit_log")
public class PreRegistrationAuditLogEntity {
    
    @Id
    private Long id;
    
    @Column("token_id")
    private Long tokenId;
    
    @Column("action")
    private String action;
    
    @Column("performed_by")
    private Long performedBy;
    
    @Column("ip_address")
    private String ipAddress;
    
    @Column("user_agent")
    private String userAgent;
    
    @Column("details")
    private String details;
    
    @Column("created_at")
    private Instant createdAt;
}
