package co.com.atlas.api.tower.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar Tower.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TowerRequest {
    private Long zoneId;
    private String code;
    private String name;
    private Integer floorsCount;
    private String description;
    private Integer sortOrder;
}
