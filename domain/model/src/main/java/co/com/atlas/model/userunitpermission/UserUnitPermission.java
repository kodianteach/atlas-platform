package co.com.atlas.model.userunitpermission;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para UserUnitPermission.
 * Representa un permiso asignado a un residente en una unidad específica.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserUnitPermission {
    private Long id;
    private Long userUnitId;
    private Long permissionId;
    private Long grantedBy;
    private Instant grantedAt;
    private Instant expiresAt;
    private Instant createdAt;
}
