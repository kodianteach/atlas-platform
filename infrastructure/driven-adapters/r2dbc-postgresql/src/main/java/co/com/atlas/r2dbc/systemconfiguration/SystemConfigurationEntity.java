package co.com.atlas.r2dbc.systemconfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para system_configuration.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("system_configuration")
public class SystemConfigurationEntity {
    
    @Id
    private Long id;
    
    @Column("config_key")
    private String configKey;
    
    @Column("config_value")
    private String configValue;
    
    private String description;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
