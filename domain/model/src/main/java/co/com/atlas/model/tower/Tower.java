package co.com.atlas.model.tower;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Tower (Torres dentro de una zona - solo CIUDADELA).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Tower {
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
    private Instant deletedAt;
}
