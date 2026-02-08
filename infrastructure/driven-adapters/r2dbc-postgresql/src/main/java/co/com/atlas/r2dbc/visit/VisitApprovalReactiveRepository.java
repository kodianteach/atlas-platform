package co.com.atlas.r2dbc.visit;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para VisitApproval.
 */
public interface VisitApprovalReactiveRepository extends ReactiveCrudRepository<VisitApprovalEntity, Long> {
    
    Flux<VisitApprovalEntity> findByVisitRequestId(Long visitRequestId);
    
    Flux<VisitApprovalEntity> findByApprovedBy(Long userId);
    
    Mono<VisitApprovalEntity> findFirstByVisitRequestIdOrderByCreatedAtDesc(Long visitRequestId);
}
