package co.com.atlas.usecase.post;

import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.gateways.PostRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Caso de uso para gestión de publicaciones.
 */
@RequiredArgsConstructor
public class PostUseCase {
    
    private final PostRepository postRepository;
    
    /**
     * Crea una nueva publicación.
     */
    public Mono<Post> create(Post post) {
        Post newPost = post.toBuilder()
                .status(PostStatus.DRAFT)
                .allowComments(post.getAllowComments() != null ? post.getAllowComments() : true)
                .isPinned(false)
                .createdAt(Instant.now())
                .build();
        return postRepository.save(newPost);
    }
    
    /**
     * Obtiene una publicación por ID.
     */
    public Mono<Post> findById(Long id) {
        return postRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Post", id)));
    }
    
    /**
     * Lista publicaciones de una organización.
     */
    public Flux<Post> findByOrganizationId(Long organizationId) {
        return postRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Lista publicaciones publicadas de una organización.
     */
    public Flux<Post> findPublishedByOrganizationId(Long organizationId) {
        return postRepository.findPublishedByOrganizationId(organizationId);
    }
    
    /**
     * Publica una publicación (cambia estado a PUBLISHED).
     */
    public Mono<Post> publish(Long id) {
        return findById(id)
                .flatMap(post -> {
                    Post updatedPost = post.toBuilder()
                            .status(PostStatus.PUBLISHED)
                            .publishedAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost);
                });
    }
    
    /**
     * Archiva una publicación.
     */
    public Mono<Post> archive(Long id) {
        return findById(id)
                .flatMap(post -> {
                    Post updatedPost = post.toBuilder()
                            .status(PostStatus.ARCHIVED)
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost);
                });
    }
    
    /**
     * Marca/desmarca como fijada.
     */
    public Mono<Post> togglePin(Long id) {
        return findById(id)
                .flatMap(post -> {
                    Post updatedPost = post.toBuilder()
                            .isPinned(!Boolean.TRUE.equals(post.getIsPinned()))
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost);
                });
    }
    
    /**
     * Actualiza una publicación.
     */
    public Mono<Post> update(Long id, Post post) {
        return findById(id)
                .flatMap(existingPost -> {
                    Post updatedPost = existingPost.toBuilder()
                            .title(post.getTitle())
                            .content(post.getContent())
                            .type(post.getType())
                            .allowComments(post.getAllowComments())
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost);
                });
    }
    
    /**
     * Elimina una publicación (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(post -> {
                    Post deletedPost = post.toBuilder()
                            .deletedAt(Instant.now())
                            .build();
                    return postRepository.save(deletedPost).then();
                });
    }
}
