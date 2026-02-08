package co.com.atlas.r2dbc.post;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PostReactiveRepository extends ReactiveCrudRepository<PostEntity, Long> {

    @Query("SELECT * FROM posts WHERE organization_id = :organizationId AND deleted_at IS NULL ORDER BY is_pinned DESC, created_at DESC")
    Flux<PostEntity> findByOrganizationId(Long organizationId);

    @Query("SELECT * FROM posts WHERE organization_id = :organizationId AND status = 'PUBLISHED' AND deleted_at IS NULL ORDER BY is_pinned DESC, published_at DESC")
    Flux<PostEntity> findPublishedByOrganizationId(Long organizationId);

    @Query("SELECT * FROM posts WHERE organization_id = :organizationId AND is_pinned = TRUE AND deleted_at IS NULL ORDER BY published_at DESC")
    Flux<PostEntity> findPinnedByOrganizationId(Long organizationId);
}
