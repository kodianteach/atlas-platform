package co.com.atlas.r2dbc.poll;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PollReactiveRepository extends ReactiveCrudRepository<PollEntity, Long> {

    @Query("SELECT * FROM polls WHERE organization_id = :organizationId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<PollEntity> findByOrganizationId(Long organizationId);

    @Query("SELECT * FROM polls WHERE organization_id = :organizationId AND status = 'ACTIVE' AND deleted_at IS NULL ORDER BY starts_at DESC")
    Flux<PollEntity> findActiveByOrganizationId(Long organizationId);
}
