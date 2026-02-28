package co.com.atlas.r2dbc.comment;

import co.com.atlas.model.comment.Comment;
import co.com.atlas.model.comment.gateways.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryAdapter implements CommentRepository {

    private final CommentReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Comment> save(Comment comment) {
        return repository.save(toEntity(comment))
                .map(this::toDomain);
    }

    @Override
    public Mono<Comment> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Flux<Comment> findByPostId(Long postId) {
        return repository.findByPostId(postId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Comment> findRepliesByParentId(Long parentId) {
        return repository.findByParentId(parentId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countByPostId(Long postId) {
        return repository.countByPostId(postId);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Flux<Comment> findFlaggedByOrganization(Long organizationId) {
        String sql = "SELECT c.* FROM comments c " +
                "INNER JOIN posts p ON c.post_id = p.id " +
                "WHERE p.organization_id = :orgId " +
                "AND c.is_approved = false " +
                "AND c.flag_reason IS NOT NULL " +
                "AND c.deleted_at IS NULL " +
                "ORDER BY c.created_at DESC";

        return databaseClient.sql(sql)
                .bind("orgId", organizationId)
                .map((row, metadata) -> Comment.builder()
                        .id(row.get("id", Long.class))
                        .postId(row.get("post_id", Long.class))
                        .authorId(row.get("author_id", Long.class))
                        .parentId(row.get("parent_id", Long.class))
                        .content(row.get("content", String.class))
                        .isApproved(row.get("is_approved", Boolean.class))
                        .flagReason(row.get("flag_reason", String.class))
                        .authorRole(row.get("author_role", String.class))
                        .createdAt(row.get("created_at", java.time.Instant.class))
                        .updatedAt(row.get("updated_at", java.time.Instant.class))
                        .deletedAt(row.get("deleted_at", java.time.Instant.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Comment> updateApproval(Long id, boolean isApproved) {
        return findById(id)
                .flatMap(comment -> {
                    Comment updated = comment.toBuilder()
                            .isApproved(isApproved)
                            .build();
                    return save(updated);
                });
    }

    @Override
    public Flux<Comment> findAllByPostId(Long postId) {
        String sql = "SELECT * FROM comments WHERE post_id = :postId AND deleted_at IS NULL ORDER BY created_at ASC";
        return databaseClient.sql(sql)
                .bind("postId", postId)
                .map((row, metadata) -> Comment.builder()
                        .id(row.get("id", Long.class))
                        .postId(row.get("post_id", Long.class))
                        .authorId(row.get("author_id", Long.class))
                        .parentId(row.get("parent_id", Long.class))
                        .content(row.get("content", String.class))
                        .isApproved(row.get("is_approved", Boolean.class))
                        .flagReason(row.get("flag_reason", String.class))
                        .authorRole(row.get("author_role", String.class))
                        .createdAt(row.get("created_at", java.time.Instant.class))
                        .updatedAt(row.get("updated_at", java.time.Instant.class))
                        .deletedAt(row.get("deleted_at", java.time.Instant.class))
                        .build())
                .all();
    }

    private Comment toDomain(CommentEntity entity) {
        return Comment.builder()
                .id(entity.getId())
                .postId(entity.getPostId())
                .authorId(entity.getAuthorId())
                .parentId(entity.getParentId())
                .content(entity.getContent())
                .isApproved(entity.getIsApproved())
                .flagReason(entity.getFlagReason())
                .authorRole(entity.getAuthorRole())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private CommentEntity toEntity(Comment comment) {
        return CommentEntity.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .isApproved(comment.getIsApproved())
                .flagReason(comment.getFlagReason())
                .authorRole(comment.getAuthorRole())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .deletedAt(comment.getDeletedAt())
                .build();
    }
}
