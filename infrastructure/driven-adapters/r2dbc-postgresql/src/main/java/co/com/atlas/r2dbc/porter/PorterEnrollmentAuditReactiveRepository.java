package co.com.atlas.r2dbc.porter;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio reactivo para porter_enrollment_audit_log.
 */
public interface PorterEnrollmentAuditReactiveRepository extends ReactiveCrudRepository<PorterEnrollmentAuditEntity, Long> {

    Flux<PorterEnrollmentAuditEntity> findByTokenId(Long tokenId);
}
