package co.com.atlas.api.zone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar Zone.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneRequest {
    private Long organizationId;
    private String code;
    private String name;
    private String description;
    private Integer sortOrder;
}
