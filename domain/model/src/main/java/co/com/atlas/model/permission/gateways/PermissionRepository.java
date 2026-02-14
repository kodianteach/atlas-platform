package co.com.atlas.model.permission.gateways;

import co.com.atlas.model.permission.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Permission.
 */
public interface PermissionRepository {
    
    /**
     * Busca un permiso por ID.
     */
    Mono<Permission> findById(Long id);
    
    /**
     * Busca un permiso por código.
     */
    Mono<Permission> findByCode(String code);
    
    /**
     * Obtiene todos los permisos asociados a un rol.
     * Los permisos se obtienen de la tabla role_permissions.
     * 
     * @param roleId ID del rol
     * @return Flux de permisos del rol
     */
    Flux<Permission> findByRoleId(Long roleId);
}
