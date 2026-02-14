package co.com.atlas.r2dbc.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para Role.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("role")
public class RoleEntity {
    
    @Id
    private Long id;
    
    private String name;
    
    private String code;
    
    private String description;
    
    @Column("module_code")
    private String moduleCode;
    
    @Column("is_system")
    private Boolean isSystem;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
