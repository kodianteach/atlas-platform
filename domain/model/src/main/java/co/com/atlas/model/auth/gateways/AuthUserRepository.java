package co.com.atlas.model.auth.gateways;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.role.Role;
import co.com.atlas.model.permission.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio para operaciones de autenticación de usuarios.
 */
public interface AuthUserRepository {
    
    /**
     * Busca un usuario por su email.
     */
    Mono<AuthUser> findByEmail(String email);
    
    /**
     * Busca un usuario por su ID.
     */
    Mono<AuthUser> findById(Long id);
    
    /**
     * Valida una contraseña contra su hash.
     */
    Mono<Boolean> validatePassword(String rawPassword, String encodedPassword);
    
    /**
     * Guarda o actualiza un usuario.
     */
    Mono<AuthUser> save(AuthUser user);
    
    /**
     * Actualiza la fecha del último login del usuario.
     */
    Mono<Void> updateLastLogin(Long userId);
    
    /**
     * Obtiene los roles de un usuario en una organización específica (multi-tenant).
     * 
     * @param userId ID del usuario
     * @param organizationId ID de la organización
     * @return Flux con los roles del usuario en esa organización
     */
    Flux<Role> findRolesByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    /**
     * Obtiene los permisos efectivos de un usuario en una organización (multi-tenant).
     * 
     * @param userId ID del usuario
     * @param organizationId ID de la organización
     * @return Flux con los permisos del usuario
     */
    Flux<Permission> findPermissionsByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    /**
     * Actualiza el last_organization_id del usuario (multi-tenant).
     * 
     * @param userId ID del usuario
     * @param organizationId ID de la última organización usada
     * @return Mono<Void> completado cuando se actualiza
     */
    Mono<Void> updateLastOrganization(Long userId, Long organizationId);
}
