package co.com.atlas.r2dbc.company;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Company.
 */
public interface CompanyReactiveRepository extends ReactiveCrudRepository<CompanyEntity, Long> {
    
    Mono<CompanyEntity> findBySlugAndDeletedAtIsNull(String slug);
    
    Flux<CompanyEntity> findByIsActiveTrueAndDeletedAtIsNull();
    
    Mono<Boolean> existsBySlugAndDeletedAtIsNull(String slug);
}
