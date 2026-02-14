package co.com.atlas.r2dbc.vehicle;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Vehicle.
 */
public interface VehicleReactiveRepository extends ReactiveCrudRepository<VehicleEntity, Long> {

    Flux<VehicleEntity> findByUnitIdAndDeletedAtIsNull(Long unitId);

    Flux<VehicleEntity> findByUnitIdAndIsActiveTrueAndDeletedAtIsNull(Long unitId);

    Mono<VehicleEntity> findByOrganizationIdAndPlateAndDeletedAtIsNull(Long organizationId, String plate);

    Mono<Long> countByUnitIdAndIsActiveTrueAndDeletedAtIsNull(Long unitId);

    Mono<Boolean> existsByOrganizationIdAndPlateAndIsActiveTrueAndDeletedAtIsNull(Long organizationId, String plate);

    Mono<Long> countByOrganizationIdAndDeletedAtIsNull(Long organizationId);
}
