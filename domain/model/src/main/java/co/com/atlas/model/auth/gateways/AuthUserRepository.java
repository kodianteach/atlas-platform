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
     * Busca un usuario por su username.
     */
    Mono<AuthUser> findByUsername(String username);
    
    /**
     * Busca un usuario por email o username (para login flexible).
     */
    Mono<AuthUser> findByEmailOrUsername(String identifier);
    
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
    
    /**
     * Verifica si existe un usuario con el tipo y número de documento especificados.
     * 
     * @param documentType código del tipo de documento (CC, NIT, etc.)
     * @param documentNumber número de documento
     * @return Mono<Boolean> true si existe
     */
    Mono<Boolean> existsByDocumentTypeAndNumber(String documentType, String documentNumber);
    
    /**
     * Busca un usuario por tipo y número de documento.
     * 
     * @param documentType código del tipo de documento
     * @param documentNumber número de documento
     * @return Mono<AuthUser> el usuario si existe
     */
    Mono<AuthUser> findByDocumentTypeAndNumber(String documentType, String documentNumber);
    
    /**
     * Verifica si existe un usuario con el email especificado.
     * 
     * @param email email a verificar
     * @return Mono<Boolean> true si existe
     */
    Mono<Boolean> existsByEmail(String email);
}
