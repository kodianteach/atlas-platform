package co.com.atlas.r2dbc.unit;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Unit.
 */
public interface UnitReactiveRepository extends ReactiveCrudRepository<UnitEntity, Long> {
    
    Mono<UnitEntity> findByOrganizationIdAndCodeAndDeletedAtIsNull(Long organizationId, String code);
    
    Flux<UnitEntity> findByOrganizationIdAndDeletedAtIsNull(Long organizationId);
    
    Flux<UnitEntity> findByTowerIdAndDeletedAtIsNull(Long towerId);
    
    Flux<UnitEntity> findByZoneIdAndDeletedAtIsNull(Long zoneId);
    
    Mono<Boolean> existsByOrganizationIdAndCodeAndDeletedAtIsNull(Long organizationId, String code);
}
