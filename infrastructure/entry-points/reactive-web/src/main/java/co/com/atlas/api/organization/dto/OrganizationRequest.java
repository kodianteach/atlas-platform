package co.com.atlas.api.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar Organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequest {
    private Long companyId;
    private String code;
    private String name;
    private String type;
    private Boolean usesZones;
    private String description;
    private String settings;
}
