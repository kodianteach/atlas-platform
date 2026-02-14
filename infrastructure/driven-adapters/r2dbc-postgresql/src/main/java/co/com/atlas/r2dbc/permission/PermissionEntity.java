package co.com.atlas.r2dbc.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para Permission.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("permissions")
public class PermissionEntity {
    
    @Id
    private Long id;
    
    private String code;
    
    private String name;
    
    private String description;
    
    @Column("module_code")
    private String moduleCode;
    
    private String resource;
    
    private String action;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
