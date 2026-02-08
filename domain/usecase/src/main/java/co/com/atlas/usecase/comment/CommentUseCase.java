package co.com.atlas.usecase.comment;

import co.com.atlas.model.comment.Comment;
import co.com.atlas.model.comment.gateways.CommentRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.gateways.PostRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Caso de uso para gestión de comentarios.
 */
@RequiredArgsConstructor
public class CommentUseCase {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    
    /**
     * Crea un nuevo comentario.
     */
    public Mono<Comment> create(Comment comment) {
        return postRepository.findById(comment.getPostId())
                .switchIfEmpty(Mono.error(new NotFoundException("Post", comment.getPostId())))
                .flatMap(post -> {
                    if (!Boolean.TRUE.equals(post.getAllowComments())) {
                        return Mono.error(new BusinessException("COMMENTS_DISABLED", "Los comentarios están deshabilitados para esta publicación"));
                    }
                    if (post.getStatus() != PostStatus.PUBLISHED) {
                        return Mono.error(new BusinessException("POST_NOT_PUBLISHED", "No se puede comentar una publicación no publicada"));
                    }
                    
                    Comment newComment = comment.toBuilder()
                            .isApproved(true)
                            .createdAt(Instant.now())
                            .build();
                    return commentRepository.save(newComment);
                });
    }
    
    /**
     * Obtiene un comentario por ID.
     */
    public Mono<Comment> findById(Long id) {
        return commentRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Comment", id)));
    }
    
    /**
     * Lista comentarios de una publicación.
     */
    public Flux<Comment> findByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }
    
    /**
     * Lista respuestas a un comentario.
     */
    public Flux<Comment> findReplies(Long parentId) {
        return commentRepository.findRepliesByParentId(parentId);
    }
    
    /**
     * Cuenta comentarios de una publicación.
     */
    public Mono<Long> countByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }
    
    /**
     * Elimina un comentario (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(comment -> {
                    Comment deleted = comment.toBuilder()
                            .deletedAt(Instant.now())
                            .build();
                    return commentRepository.save(deleted).then();
                });
    }
}
