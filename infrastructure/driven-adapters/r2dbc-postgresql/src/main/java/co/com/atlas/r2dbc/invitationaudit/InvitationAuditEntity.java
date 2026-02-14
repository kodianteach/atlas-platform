package co.com.atlas.r2dbc.invitationaudit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para invitation_audit_log.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("invitation_audit_log")
public class InvitationAuditEntity {
    
    @Id
    private Long id;
    
    @Column("invitation_id")
    private Long invitationId;
    
    private String action;
    
    @Column("performed_by")
    private Long performedBy;
    
    @Column("old_status")
    private String oldStatus;
    
    @Column("new_status")
    private String newStatus;
    
    private String details;
    
    @Column("ip_address")
    private String ipAddress;
    
    @Column("user_agent")
    private String userAgent;
    
    @Column("created_at")
    private Instant createdAt;
}
