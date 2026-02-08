package co.com.atlas.api.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para Organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    private Long id;
    private Long companyId;
    private String code;
    private String name;
    private String slug;
    private String type;
    private Boolean usesZones;
    private String description;
    private String settings;
    private String status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
