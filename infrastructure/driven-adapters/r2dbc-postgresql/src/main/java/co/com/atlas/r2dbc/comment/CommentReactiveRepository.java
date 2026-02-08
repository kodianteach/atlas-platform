package co.com.atlas.r2dbc.comment;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentReactiveRepository extends ReactiveCrudRepository<CommentEntity, Long> {

    @Query("SELECT * FROM comments WHERE post_id = :postId AND deleted_at IS NULL ORDER BY created_at ASC")
    Flux<CommentEntity> findByPostId(Long postId);

    @Query("SELECT * FROM comments WHERE parent_id = :parentId AND deleted_at IS NULL ORDER BY created_at ASC")
    Flux<CommentEntity> findByParentId(Long parentId);

    @Query("SELECT COUNT(*) FROM comments WHERE post_id = :postId AND deleted_at IS NULL")
    Mono<Long> countByPostId(Long postId);
}
