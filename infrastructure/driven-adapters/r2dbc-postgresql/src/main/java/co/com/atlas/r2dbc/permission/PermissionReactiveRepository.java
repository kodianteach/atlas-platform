package co.com.atlas.r2dbc.permission;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Permission.
 */
public interface PermissionReactiveRepository extends ReactiveCrudRepository<PermissionEntity, Long> {
    
    Mono<PermissionEntity> findByCode(String code);
    
    /**
     * Obtiene todos los permisos asociados a un rol.
     * Usa una consulta con JOIN a la tabla role_permissions.
     * 
     * @param roleId ID del rol
     * @return Flux de permisos del rol
     */
    @Query("""
        SELECT p.* FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permission_id
        WHERE rp.role_id = :roleId
    """)
    Flux<PermissionEntity> findByRoleId(Long roleId);
}
