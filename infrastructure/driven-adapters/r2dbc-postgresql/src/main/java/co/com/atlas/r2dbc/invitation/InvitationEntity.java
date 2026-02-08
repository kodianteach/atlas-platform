package co.com.atlas.r2dbc.invitation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para Invitation.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("invitation")
public class InvitationEntity {
    
    @Id
    private Long id;
    
    @Column("organization_id")
    private Long organizationId;
    
    @Column("unit_id")
    private Long unitId;
    
    private String email;
    
    @Column("phone_number")
    private String phoneNumber;
    
    @Column("invitation_token")
    private String invitationToken;
    
    private String type;
    
    @Column("role_id")
    private Long roleId;
    
    private String status;
    
    @Column("invited_by")
    private Long invitedBy;
    
    @Column("expires_at")
    private Instant expiresAt;
    
    @Column("accepted_at")
    private Instant acceptedAt;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
