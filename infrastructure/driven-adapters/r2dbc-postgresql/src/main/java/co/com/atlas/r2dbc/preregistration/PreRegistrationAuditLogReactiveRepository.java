package co.com.atlas.r2dbc.preregistration;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio reactivo para preregistration_audit_log.
 */
public interface PreRegistrationAuditLogReactiveRepository 
        extends ReactiveCrudRepository<PreRegistrationAuditLogEntity, Long> {
    
    /**
     * Busca registros de auditoría por token.
     */
    Flux<PreRegistrationAuditLogEntity> findByTokenId(Long tokenId);
    
    /**
     * Busca registros de auditoría por acción.
     */
    Flux<PreRegistrationAuditLogEntity> findByAction(String action);
}
