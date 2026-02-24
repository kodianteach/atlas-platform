package co.com.atlas.r2dbc.authuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para User (tabla users).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class AuthUserEntity {
    
    @Id
    private Long id;
    
    private String names;
    
    private String email;
    
    private String username;
    
    @Column("password_hash")
    private String passwordHash;
    
    private String phone;
    
    @Column("document_type")
    private String documentType;
    
    @Column("document_number")
    private String documentNumber;
    
    @Column("is_active")
    private boolean active;
    
    @Column("status")
    private String status;
    
    @Column("last_login_at")
    private Instant lastLoginAt;
    
    @Column("last_organization_id")
    private Long lastOrganizationId;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
    
    @Column("deleted_at")
    private Instant deletedAt;
}
