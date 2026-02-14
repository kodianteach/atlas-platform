package co.com.atlas.model.userorganization.gateways;

import co.com.atlas.model.userorganization.UserOrganization;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de UserOrganization.
 */
public interface UserOrganizationRepository {
    
    /**
     * Busca una membresía por ID.
     */
    Mono<UserOrganization> findById(Long id);
    
    /**
     * Busca la membresía de un usuario en una organización.
     */
    Mono<UserOrganization> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    /**
     * Lista las organizaciones de un usuario.
     */
    Flux<UserOrganization> findByUserId(Long userId);
    
    /**
     * Lista los usuarios de una organización.
     */
    Flux<UserOrganization> findByOrganizationId(Long organizationId);
    
    /**
     * Lista las organizaciones activas de un usuario.
     */
    Flux<UserOrganization> findActiveByUserId(Long userId);
    
    /**
     * Guarda o actualiza una membresía.
     */
    Mono<UserOrganization> save(UserOrganization userOrganization);
    
    /**
     * Elimina una membresía.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si un usuario pertenece a una organización.
     */
    Mono<Boolean> existsByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    /**
     * Cuenta los usuarios activos de una organización.
     */
    Mono<Long> countActiveByOrganization(Long organizationId);
    
    /**
     * Actualiza el estado de una vinculación usuario-organización.
     * 
     * @param userId ID del usuario
     * @param organizationId ID de la organización
     * @param status nuevo estado (ACTIVE, PENDING, etc.)
     * @return Mono vacío cuando se completa
     */
    Mono<Void> updateStatusByUserIdAndOrganizationId(Long userId, Long organizationId, String status);
}
