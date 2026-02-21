package co.com.atlas.model.porter.gateways;

import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para auditoría de enrolamiento de porteros.
 */
public interface PorterEnrollmentAuditRepository {

    /**
     * Guarda un registro de auditoría.
     */
    Mono<PorterEnrollmentAuditLog> save(PorterEnrollmentAuditLog auditLog);

    /**
     * Busca registros de auditoría por token ID.
     */
    Flux<PorterEnrollmentAuditLog> findByTokenId(Long tokenId);
}
