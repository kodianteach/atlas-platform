package co.com.atlas.r2dbc.access;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Spring Data R2DBC reactive repository para access_events.
 */
public interface AccessEventReactiveRepository extends ReactiveCrudRepository<AccessEventEntity, Long> {

    Flux<AccessEventEntity> findByOrganizationId(Long organizationId);

    Flux<AccessEventEntity> findByAuthorizationId(Long authorizationId);

    Flux<AccessEventEntity> findByPorterUserId(Long porterUserId);
}
