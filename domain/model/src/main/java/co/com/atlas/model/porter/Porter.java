package co.com.atlas.model.porter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Proyección de dominio para Portero.
 * Un portero es un usuario (tabla users) con rol PORTERO_GENERAL o PORTERO_DELIVERY
 * asignado via user_roles_multi. No tiene tabla propia.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Porter {
    private Long id;
    private String names;
    private String email;
    private PorterType porterType;
    private String status;
    private Long organizationId;
    private Instant createdAt;
    private Instant updatedAt;
}
