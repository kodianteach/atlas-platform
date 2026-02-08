package co.com.atlas.api.tower.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para Tower.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TowerResponse {
    private Long id;
    private Long zoneId;
    private String code;
    private String name;
    private Integer floorsCount;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
