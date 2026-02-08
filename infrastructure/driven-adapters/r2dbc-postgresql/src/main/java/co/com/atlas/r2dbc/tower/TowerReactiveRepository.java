package co.com.atlas.r2dbc.tower;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Tower.
 */
public interface TowerReactiveRepository extends ReactiveCrudRepository<TowerEntity, Long> {
    
    Mono<TowerEntity> findByZoneIdAndCodeAndDeletedAtIsNull(Long zoneId, String code);
    
    Flux<TowerEntity> findByZoneIdAndDeletedAtIsNullOrderBySortOrder(Long zoneId);
    
    Flux<TowerEntity> findByZoneIdAndIsActiveTrueAndDeletedAtIsNullOrderBySortOrder(Long zoneId);
    
    Mono<Boolean> existsByZoneIdAndCodeAndDeletedAtIsNull(Long zoneId, String code);
}
