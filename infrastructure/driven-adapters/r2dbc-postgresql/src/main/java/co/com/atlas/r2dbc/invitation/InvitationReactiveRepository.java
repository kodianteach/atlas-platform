package co.com.atlas.r2dbc.invitation;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Invitation.
 */
public interface InvitationReactiveRepository extends ReactiveCrudRepository<InvitationEntity, Long> {
    
    Mono<InvitationEntity> findByInvitationToken(String token);
    
    Flux<InvitationEntity> findByOrganizationId(Long organizationId);
    
    Flux<InvitationEntity> findByUnitId(Long unitId);
    
    Flux<InvitationEntity> findByEmail(String email);
    
    Flux<InvitationEntity> findByEmailAndStatus(String email, String status);
    
    Mono<Boolean> existsByEmailAndOrganizationIdAndStatus(String email, Long organizationId, String status);
}
