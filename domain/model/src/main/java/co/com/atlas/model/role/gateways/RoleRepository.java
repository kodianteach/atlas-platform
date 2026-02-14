package co.com.atlas.model.role.gateways;

import co.com.atlas.model.role.Role;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Role.
 */
public interface RoleRepository {
    
    /**
     * Busca un rol por su código.
     * 
     * @param code Código del rol (ej: ADMIN_ATLAS, OWNER, TENANT)
     * @return El rol encontrado o empty
     */
    Mono<Role> findByCode(String code);
    
    /**
     * Busca un rol por ID.
     */
    Mono<Role> findById(Long id);
}
