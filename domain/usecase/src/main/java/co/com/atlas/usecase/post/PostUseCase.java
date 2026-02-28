package co.com.atlas.usecase.post;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.common.PostPollFilter;
import co.com.atlas.model.notification.Notification;
import co.com.atlas.model.notification.NotificationType;
import co.com.atlas.model.notification.gateways.NotificationRepository;
import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.gateways.PostRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Caso de uso para gestión de publicaciones.
 */
@RequiredArgsConstructor
public class PostUseCase {
    
    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_CONTENT_LENGTH = 5000;
    
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    
    /**
     * Crea una nueva publicación con validaciones de negocio.
     */
    public Mono<Post> create(Post post) {
        return validatePost(post)
                .then(Mono.defer(() -> {
                    Post newPost = post.toBuilder()
                            .status(PostStatus.DRAFT)
                            .allowComments(post.getAllowComments() != null ? post.getAllowComments() : true)
                            .isPinned(false)
                            .createdAt(Instant.now())
                            .build();
                    return postRepository.save(newPost);
                }));
    }
    
    /**
     * Obtiene una publicación por ID.
     */
    public Mono<Post> findById(Long id) {
        return postRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Post", id)));
    }
    
    /**
     * Obtiene una publicación por ID verificando pertenencia a organización (BOLA prevention).
     */
    public Mono<Post> findByIdAndOrganization(Long id, Long organizationId) {
        return findById(id)
                .flatMap(post -> {
                    if (!post.getOrganizationId().equals(organizationId)) {
                        return Mono.error(new BusinessException("ACCESS_DENIED", "No tiene acceso a este recurso"));
                    }
                    return Mono.just(post);
                });
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
     * Actualiza una publicación con validaciones.
     */
    public Mono<Post> update(Long id, Post post) {
        return validatePost(post)
                .then(findById(id))
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
     * Publica una publicación verificando ownership organizacional.
     * Dispara notificación broadcast a la organización.
     */
    public Mono<Post> publish(Long id, Long organizationId) {
        return findByIdAndOrganization(id, organizationId)
                .flatMap(post -> {
                    Post updatedPost = post.toBuilder()
                            .status(PostStatus.PUBLISHED)
                            .publishedAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost)
                            .flatMap(saved -> {
                                Notification notification = Notification.builder()
                                        .organizationId(saved.getOrganizationId())
                                        .title("Nueva publicación")
                                        .message(saved.getTitle())
                                        .type(NotificationType.POST_PUBLISHED)
                                        .isRead(false)
                                        .entityType("POST")
                                        .entityId(saved.getId())
                                        .createdAt(Instant.now())
                                        .build();
                                return notificationRepository.save(notification)
                                        .thenReturn(saved);
                            });
                });
    }
    
    /**
     * Elimina una publicación (soft delete) verificando ownership organizacional.
     */
    public Mono<Void> delete(Long id, Long organizationId) {
        return findByIdAndOrganization(id, organizationId)
                .flatMap(post -> {
                    Post deletedPost = post.toBuilder()
                            .deletedAt(Instant.now())
                            .build();
                    return postRepository.save(deletedPost).then();
                });
    }
    
    /**
     * Reactiva una publicación archivada (ARCHIVED → PUBLISHED) con verificación organizacional.
     */
    public Mono<Post> reactivate(Long id, Long organizationId) {
        return findByIdAndOrganization(id, organizationId)
                .flatMap(post -> {
                    if (post.getStatus() != PostStatus.ARCHIVED) {
                        return Mono.error(new BusinessException("INVALID_STATE", "Solo se pueden reactivar publicaciones archivadas"));
                    }
                    Post updatedPost = post.toBuilder()
                            .status(PostStatus.PUBLISHED)
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost);
                });
    }

    /**
     * Archiva una publicación con verificación organizacional (BOLA prevention).
     */
    public Mono<Post> archive(Long id, Long organizationId) {
        return findByIdAndOrganization(id, organizationId)
                .flatMap(post -> {
                    if (post.getStatus() != PostStatus.PUBLISHED) {
                        return Mono.error(new BusinessException("INVALID_STATE", "Solo se pueden archivar publicaciones publicadas"));
                    }
                    Post updatedPost = post.toBuilder()
                            .status(PostStatus.ARCHIVED)
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost);
                });
    }

    /**
     * Toggle pin con verificación organizacional (BOLA prevention).
     */
    public Mono<Post> togglePin(Long id, Long organizationId) {
        return findByIdAndOrganization(id, organizationId)
                .flatMap(post -> {
                    Post updatedPost = post.toBuilder()
                            .isPinned(!Boolean.TRUE.equals(post.getIsPinned()))
                            .updatedAt(Instant.now())
                            .build();
                    return postRepository.save(updatedPost);
                });
    }

    /**
     * Búsqueda paginada de posts por filtros dinámicos para panel admin.
     */
    public Mono<PageResponse<Post>> findByFilters(Long organizationId, PostPollFilter filter) {
        return postRepository.findByFilters(organizationId, filter);
    }

    /**
     * Conteo de posts por estado para estadísticas del panel admin.
     */
    public Mono<Map<String, Long>> countByStatus(Long organizationId) {
        return postRepository.countByStatusAndOrganization(organizationId);
    }

    /**
     * Valida restricciones de negocio para título y contenido.
     */
    private Mono<Void> validatePost(Post post) {
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            return Mono.error(new BusinessException("INVALID_TITLE", "El título es requerido"));
        }
        if (post.getTitle().length() > MAX_TITLE_LENGTH) {
            return Mono.error(new BusinessException("TITLE_TOO_LONG", 
                    "El título no puede exceder " + MAX_TITLE_LENGTH + " caracteres"));
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            return Mono.error(new BusinessException("INVALID_CONTENT", "El contenido es requerido"));
        }
        if (post.getContent().length() > MAX_CONTENT_LENGTH) {
            return Mono.error(new BusinessException("CONTENT_TOO_LONG", 
                    "El contenido no puede exceder " + MAX_CONTENT_LENGTH + " caracteres"));
        }
        return Mono.empty();
    }
}
