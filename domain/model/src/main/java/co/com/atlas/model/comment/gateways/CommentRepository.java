package co.com.atlas.model.comment.gateways;

import co.com.atlas.model.comment.Comment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para repositorio de Comments.
 */
public interface CommentRepository {
    
    Mono<Comment> save(Comment comment);
    
    Mono<Comment> findById(Long id);
    
    Flux<Comment> findByPostId(Long postId);
    
    Flux<Comment> findRepliesByParentId(Long parentId);
    
    Mono<Long> countByPostId(Long postId);
    
    Mono<Void> deleteById(Long id);

    Flux<Comment> findFlaggedByOrganization(Long organizationId);

    Mono<Comment> updateApproval(Long id, boolean isApproved);

    Flux<Comment> findAllByPostId(Long postId);
}
