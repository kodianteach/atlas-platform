package co.com.atlas.r2dbc.comment;

import co.com.atlas.model.comment.Comment;
import co.com.atlas.model.comment.gateways.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryAdapter implements CommentRepository {

    private final CommentReactiveRepository repository;

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

    private Comment toDomain(CommentEntity entity) {
        return Comment.builder()
                .id(entity.getId())
                .postId(entity.getPostId())
                .authorId(entity.getAuthorId())
                .parentId(entity.getParentId())
                .content(entity.getContent())
                .isApproved(entity.getIsApproved())
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
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .deletedAt(comment.getDeletedAt())
                .build();
    }
}
