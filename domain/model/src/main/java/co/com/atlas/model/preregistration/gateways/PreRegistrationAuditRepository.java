package co.com.atlas.model.preregistration.gateways;

import co.com.atlas.model.preregistration.PreRegistrationAuditLog;
import co.com.atlas.model.preregistration.PreRegistrationAuditAction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de auditoría de pre-registros.
 */
public interface PreRegistrationAuditRepository {
    
    /**
     * Guarda un registro de auditoría.
     */
    Mono<PreRegistrationAuditLog> save(PreRegistrationAuditLog audit);
    
    /**
     * Lista registros de auditoría por token.
     */
    Flux<PreRegistrationAuditLog> findByTokenId(Long tokenId);
    
    /**
     * Lista registros de auditoría por acción.
     */
    Flux<PreRegistrationAuditLog> findByAction(PreRegistrationAuditAction action);
}
