package co.com.atlas.r2dbc.invitationaudit;

import co.com.atlas.model.invitation.gateways.InvitationAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway InvitationAuditRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class InvitationAuditRepositoryAdapter implements InvitationAuditRepository {

    private final InvitationAuditReactiveRepository repository;

    @Override
    public Mono<Void> logAction(Long invitationId, String action, Long performedBy, 
                                 String oldStatus, String newStatus, String details) {
        InvitationAuditEntity entity = InvitationAuditEntity.builder()
                .invitationId(invitationId)
                .action(action)
                .performedBy(performedBy)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .details(details)
                .createdAt(Instant.now())
                .build();
        
        return repository.save(entity).then();
    }
}
