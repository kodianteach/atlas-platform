package co.com.atlas.r2dbc.visit;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repositorio reactivo para VisitRequest.
 */
public interface VisitRequestReactiveRepository extends ReactiveCrudRepository<VisitRequestEntity, Long> {
    
    Flux<VisitRequestEntity> findByOrganizationId(Long organizationId);
    
    Flux<VisitRequestEntity> findByUnitId(Long unitId);
    
    Flux<VisitRequestEntity> findByRequestedBy(Long userId);
    
    Flux<VisitRequestEntity> findByStatus(String status);
    
    Flux<VisitRequestEntity> findByOrganizationIdAndStatus(Long organizationId, String status);
    
    Flux<VisitRequestEntity> findByUnitIdAndValidUntilAfter(Long unitId, Instant now);
    
    Mono<Long> countByUnitIdAndStatus(Long unitId, String status);
}
