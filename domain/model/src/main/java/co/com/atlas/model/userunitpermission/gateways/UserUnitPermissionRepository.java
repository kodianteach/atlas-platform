package co.com.atlas.model.userunitpermission.gateways;

import co.com.atlas.model.userunitpermission.UserUnitPermission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de UserUnitPermission.
 */
public interface UserUnitPermissionRepository {

    /**
     * Guarda un permiso de usuario-unidad.
     */
    Mono<UserUnitPermission> save(UserUnitPermission permission);

    /**
     * Busca todos los permisos de un usuario-unidad.
     */
    Flux<UserUnitPermission> findByUserUnitId(Long userUnitId);

    /**
     * Elimina todos los permisos de un usuario-unidad.
     */
    Mono<Void> deleteByUserUnitId(Long userUnitId);
}
