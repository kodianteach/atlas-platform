package co.com.atlas.r2dbc.organizationconfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para organization_configuration.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("organization_configuration")
public class OrganizationConfigurationEntity {

    @Id
    private Long id;

    @Column("organization_id")
    private Long organizationId;

    @Column("max_units_per_distribution")
    private Integer maxUnitsPerDistribution;

    @Column("enable_owner_permission_management")
    private Boolean enableOwnerPermissionManagement;

    // Branding fields (HU #10)

    @Column("logo_data")
    private byte[] logoData;

    @Column("logo_content_type")
    private String logoContentType;

    @Column("dominant_color")
    private String dominantColor;

    @Column("secondary_color")
    private String secondaryColor;

    @Column("accent_color")
    private String accentColor;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
