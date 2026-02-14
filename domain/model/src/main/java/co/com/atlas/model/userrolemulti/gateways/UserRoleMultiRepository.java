package co.com.atlas.model.userrolemulti.gateways;

import co.com.atlas.model.userrolemulti.UserRoleMulti;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de UserRoleMulti.
 */
public interface UserRoleMultiRepository {
    
    /**
     * Guarda una asignación de rol a usuario en organización.
     */
    Mono<UserRoleMulti> save(UserRoleMulti userRoleMulti);
    
    /**
     * Busca los roles de un usuario en una organización.
     */
    Flux<UserRoleMulti> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    /**
     * Busca todos los roles de un usuario (en todas las organizaciones).
     */
    Flux<UserRoleMulti> findByUserId(Long userId);
    
    /**
     * Busca los roles de un usuario que no tienen organización asignada (organization_id IS NULL).
     * Estos son roles asignados durante pre-registro, antes del onboarding.
     */
    Flux<UserRoleMulti> findByUserIdAndOrganizationIdIsNull(Long userId);
    
    /**
     * Verifica si existe la asignación de un rol específico a un usuario en una organización.
     */
    Mono<Boolean> existsByUserIdAndOrganizationIdAndRoleId(Long userId, Long organizationId, Long roleId);
    
    /**
     * Elimina una asignación de rol.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Elimina todas las asignaciones de rol de un usuario en una organización.
     */
    Mono<Void> deleteByUserIdAndOrganizationId(Long userId, Long organizationId);
}
