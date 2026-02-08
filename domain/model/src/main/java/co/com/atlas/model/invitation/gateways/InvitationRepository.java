package co.com.atlas.model.invitation.gateways;

import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Invitation.
 */
public interface InvitationRepository {
    
    /**
     * Busca una invitación por ID.
     */
    Mono<Invitation> findById(Long id);
    
    /**
     * Busca una invitación por token.
     */
    Mono<Invitation> findByToken(String token);
    
    /**
     * Lista las invitaciones de una organización.
     */
    Flux<Invitation> findByOrganizationId(Long organizationId);
    
    /**
     * Lista las invitaciones de una unidad.
     */
    Flux<Invitation> findByUnitId(Long unitId);
    
    /**
     * Lista las invitaciones para un email específico.
     */
    Flux<Invitation> findByEmail(String email);
    
    /**
     * Lista las invitaciones pendientes para un email.
     */
    Flux<Invitation> findPendingByEmail(String email);
    
    /**
     * Guarda o actualiza una invitación.
     */
    Mono<Invitation> save(Invitation invitation);
    
    /**
     * Elimina una invitación.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si existe una invitación pendiente para el email en la organización.
     */
    Mono<Boolean> existsPendingByEmailAndOrganizationId(String email, Long organizationId);
}
