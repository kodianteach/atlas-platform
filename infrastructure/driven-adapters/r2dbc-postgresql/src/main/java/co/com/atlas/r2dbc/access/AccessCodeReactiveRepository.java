package co.com.atlas.r2dbc.access;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para AccessCode.
 */
public interface AccessCodeReactiveRepository extends ReactiveCrudRepository<AccessCodeEntity, Long> {
    
    Mono<AccessCodeEntity> findByCodeHash(String codeHash);
    
    Flux<AccessCodeEntity> findByVisitRequestId(Long visitRequestId);
    
    Flux<AccessCodeEntity> findByStatus(String status);
    
    Mono<Boolean> existsByCodeHash(String codeHash);
}
