package co.com.atlas.r2dbc.zone;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Zone.
 */
public interface ZoneReactiveRepository extends ReactiveCrudRepository<ZoneEntity, Long> {
    
    Mono<ZoneEntity> findByOrganizationIdAndCodeAndDeletedAtIsNull(Long organizationId, String code);
    
    Flux<ZoneEntity> findByOrganizationIdAndDeletedAtIsNullOrderBySortOrder(Long organizationId);
    
    Flux<ZoneEntity> findByOrganizationIdAndIsActiveTrueAndDeletedAtIsNullOrderBySortOrder(Long organizationId);
    
    Mono<Boolean> existsByOrganizationIdAndCodeAndDeletedAtIsNull(Long organizationId, String code);
}
