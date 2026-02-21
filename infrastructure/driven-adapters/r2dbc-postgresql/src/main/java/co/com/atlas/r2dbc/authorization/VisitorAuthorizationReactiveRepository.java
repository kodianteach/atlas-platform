package co.com.atlas.r2dbc.authorization;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio reactivo R2DBC para visitor_authorizations.
 */
public interface VisitorAuthorizationReactiveRepository
        extends ReactiveCrudRepository<VisitorAuthorizationEntity, Long> {

    Flux<VisitorAuthorizationEntity> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Flux<VisitorAuthorizationEntity> findByUnitIdOrderByCreatedAtDesc(Long unitId);

    Flux<VisitorAuthorizationEntity> findByUnitIdAndCreatedByUserIdOrderByCreatedAtDesc(
            Long unitId, Long createdByUserId);

    Flux<VisitorAuthorizationEntity> findByCreatedByUserIdOrderByCreatedAtDesc(Long createdByUserId);
}
