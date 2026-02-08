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
 * Entidad R2DBC para admin_activation_tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("admin_activation_tokens")
public class AdminActivationTokenEntity {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("token_hash")
    private String tokenHash;
    
    @Column("initial_password_hash")
    private String initialPasswordHash;
    
    @Column("expires_at")
    private Instant expiresAt;
    
    @Column("consumed_at")
    private Instant consumedAt;
    
    @Column("status")
    private String status;
    
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
    
    @Column("metadata")
    private String metadata;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
