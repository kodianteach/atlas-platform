package co.com.atlas.r2dbc.invitationaudit;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio reactivo para InvitationAuditEntity.
 */
public interface InvitationAuditReactiveRepository extends ReactiveCrudRepository<InvitationAuditEntity, Long> {
    
    Flux<InvitationAuditEntity> findByInvitationIdOrderByCreatedAtDesc(Long invitationId);
}
