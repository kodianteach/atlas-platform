package co.com.atlas.model.invitation.gateways;

import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationFilters;
import co.com.atlas.model.invitation.InvitationMailStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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
    
    /**
     * Busca invitaciones por lista de IDs.
     * 
     * @param ids lista de IDs de invitaciones
     * @return invitaciones encontradas
     */
    Flux<Invitation> findByIdIn(java.util.List<Long> ids);
    
    /**
     * Actualiza el estado de envío de invitación.
     * 
     * @param id ID de la invitación
     * @param mailStatus nuevo estado de envío (PENDING, SENT, FAILED)
     * @param sentAt timestamp del envío
     * @return invitación actualizada
     */
    Mono<Invitation> updateMailStatus(Long id, String mailStatus, java.time.Instant sentAt);
    
    /**
     * Incrementa el contador de reintentos.
     * 
     * @param id ID de la invitación
     * @return invitación actualizada
     */
    Mono<Invitation> incrementRetryCount(Long id);
    
    /**
     * Busca invitaciones pendientes de una organización que no han sido enviadas o fallaron.
     * 
     * @param organizationId ID de la organización
     * @return invitaciones pendientes de envío
     */
    Flux<Invitation> findPendingMailByOrganizationId(Long organizationId);
    
    /**
     * Lista invitaciones por organización y unidad.
     * 
     * @param organizationId ID de la organización
     * @param unitId ID de la unidad
     * @return invitaciones de la unidad
     */
    Flux<Invitation> findByOrganizationIdAndUnitId(Long organizationId, Long unitId);
    
    /**
     * Verifica si existe una invitación pendiente para el email y unidad.
     * 
     * @param email email del invitado
     * @param unitId ID de la unidad
     * @return true si existe
     */
    Mono<Boolean> existsPendingByEmailAndUnitId(String email, Long unitId);
    
    /**
     * Searches invitations by organization with dynamic filters.
     * Used for admin invitation history with filtering support.
     *
     * @param organizationId the organization ID
     * @param filters optional filter criteria
     * @return matching invitations
     */
    Flux<Invitation> findByOrganizationIdWithFilters(Long organizationId, InvitationFilters filters);
    
    /**
     * Searches invitations by unit with dynamic filters.
     * Used for owner/resident invitation history scoped to their unit.
     *
     * @param unitId the unit ID
     * @param filters optional filter criteria
     * @return matching invitations
     */
    Flux<Invitation> findByUnitIdWithFilters(Long unitId, InvitationFilters filters);
}
