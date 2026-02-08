package co.com.atlas.model.zone;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Zone (Zonas dentro de una organización).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Zone {
    private Long id;
    private Long organizationId;
    private String code;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
