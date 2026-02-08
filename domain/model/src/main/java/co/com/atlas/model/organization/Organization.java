package co.com.atlas.model.organization;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Organization (Ciudadela o Conjunto Residencial).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Organization {
    private Long id;
    private Long companyId;
    private String code;
    private String name;
    private String slug;
    private OrganizationType type;
    private Boolean usesZones;
    private String description;
    private String settings; // JSON como String
    private String status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
