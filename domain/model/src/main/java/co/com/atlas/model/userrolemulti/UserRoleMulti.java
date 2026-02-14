package co.com.atlas.model.userrolemulti;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para UserRoleMulti (Roles por usuario y organización).
 * 
 * Representa la asignación de roles a usuarios en contextos multi-tenant,
 * donde un usuario puede tener diferentes roles en diferentes organizaciones.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserRoleMulti {
    private Long id;
    private Long userId;
    private Long organizationId;
    private Long roleId;
    private Boolean isPrimary;
    private Instant assignedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
