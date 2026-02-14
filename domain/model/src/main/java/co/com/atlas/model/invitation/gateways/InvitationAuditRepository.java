package co.com.atlas.model.invitation.gateways;

import reactor.core.publisher.Mono;

/**
 * Gateway para log de auditoría de invitaciones.
 */
public interface InvitationAuditRepository {
    
    /**
     * Registra una acción de auditoría para una invitación.
     * 
     * @param invitationId ID de la invitación
     * @param action acción realizada (CREATED, SENT, RESENT, ACCEPTED, EXPIRED, CANCELLED, FAILED)
     * @param performedBy ID del usuario que realizó la acción (puede ser null)
     * @param ipAddress dirección IP (puede ser null)
     * @param userAgent user agent del navegador (puede ser null)
     * @param metadata metadata adicional en JSON (puede ser null)
     * @return Mono<Void> completado cuando se registra
     */
    Mono<Void> logAction(Long invitationId, String action, Long performedBy, 
                         String ipAddress, String userAgent, String metadata);
    
    /**
     * Registra una acción de auditoría sin información de request.
     * 
     * @param invitationId ID de la invitación
     * @param action acción realizada
     * @param performedBy ID del usuario que realizó la acción
     * @return Mono<Void> completado cuando se registra
     */
    default Mono<Void> logAction(Long invitationId, String action, Long performedBy) {
        return logAction(invitationId, action, performedBy, null, null, null);
    }
    
    // Constantes para acciones de auditoría
    String ACTION_CREATED = "CREATED";
    String ACTION_SENT = "SENT";
    String ACTION_RESENT = "RESENT";
    String ACTION_ACCEPTED = "ACCEPTED";
    String ACTION_EXPIRED = "EXPIRED";
    String ACTION_CANCELLED = "CANCELLED";
    String ACTION_FAILED = "FAILED";
}
