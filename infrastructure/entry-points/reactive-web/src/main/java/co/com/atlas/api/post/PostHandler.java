package co.com.atlas.api.post;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.post.dto.PostRequest;
import co.com.atlas.api.post.dto.PostResponse;
import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.common.PostPollFilter;
import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostType;
import co.com.atlas.usecase.comment.CommentUseCase;
import co.com.atlas.usecase.post.PostUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PostHandler {

    private final PostUseCase postUseCase;
    private final CommentUseCase commentUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        Long authorId = Long.valueOf(request.headers().firstHeader("X-User-Id"));
        
        return request.bodyToMono(PostRequest.class)
                .map(req -> Post.builder()
                        .organizationId(req.getOrganizationId())
                        .authorId(authorId)
                        .title(req.getTitle())
                        .content(req.getContent())
                        .type(req.getType() != null ? PostType.valueOf(req.getType()) : PostType.ANNOUNCEMENT)
                        .allowComments(req.getAllowComments())
                        .build())
                .flatMap(postUseCase::create)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return postUseCase.findById(id)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> findByOrganizationId(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return postUseCase.findByOrganizationId(organizationId)
                .flatMap(this::enrichWithCommentsCount)
                .collectList()
                .flatMap(posts -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(posts, "Posts obtenidos exitosamente")));
    }

    public Mono<ServerResponse> findPublished(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return postUseCase.findPublishedByOrganizationId(organizationId)
                .flatMap(this::enrichWithCommentsCount)
                .collectList()
                .flatMap(posts -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(posts, "Posts publicados obtenidos exitosamente")));
    }

    public Mono<ServerResponse> publish(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return postUseCase.publish(id, organizationId)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> archive(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return postUseCase.archive(id, organizationId)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> togglePin(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return postUseCase.togglePin(id, organizationId)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return request.bodyToMono(PostRequest.class)
                .map(req -> Post.builder()
                        .title(req.getTitle())
                        .content(req.getContent())
                        .type(req.getType() != null ? PostType.valueOf(req.getType()) : null)
                        .allowComments(req.getAllowComments())
                        .build())
                .flatMap(post -> postUseCase.update(id, post))
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return postUseCase.delete(id, organizationId)
                .then(ServerResponse.ok().bodyValue(ApiResponse.success(null, "Post eliminado exitosamente")));
    }

    /**
     * Búsqueda paginada de posts con filtros dinámicos para panel admin.
     */
    public Mono<ServerResponse> searchPosts(ServerRequest request) {
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        PostPollFilter filter = PostPollFilter.builder()
                .type(request.queryParam("type").orElse(null))
                .status(request.queryParam("status").orElse(null))
                .dateFrom(request.queryParam("dateFrom").map(Instant::parse).orElse(null))
                .dateTo(request.queryParam("dateTo").map(Instant::parse).orElse(null))
                .search(request.queryParam("search").orElse(null))
                .page(request.queryParam("page").map(Integer::parseInt).orElse(0))
                .size(request.queryParam("size").map(Integer::parseInt).orElse(10))
                .build();

        return postUseCase.findByFilters(organizationId, filter)
                .flatMap(pageResponse ->
                    Flux.fromIterable(pageResponse.getContent())
                            .flatMap(this::enrichWithCommentsCount)
                            .collectList()
                            .map(enrichedPosts -> PageResponse.<PostResponse>builder()
                                    .content(enrichedPosts)
                                    .page(pageResponse.getPage())
                                    .size(pageResponse.getSize())
                                    .totalElements(pageResponse.getTotalElements())
                                    .totalPages(pageResponse.getTotalPages())
                                    .build())
                )
                .flatMap(pageResponse -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(pageResponse, "Posts obtenidos exitosamente")));
    }

    /**
     * Reactiva una publicación archivada.
     */
    public Mono<ServerResponse> reactivatePost(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return postUseCase.reactivate(id, organizationId)
                .flatMap(this::buildSuccessResponse);
    }

    /**
     * Obtiene estadísticas de posts por estado para la organización.
     */
    public Mono<ServerResponse> getPostStats(ServerRequest request) {
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return postUseCase.countByStatus(organizationId)
                .flatMap(stats -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(stats, "Estadísticas obtenidas exitosamente")));
    }

    private Mono<ServerResponse> buildSuccessResponse(Post post) {
        return enrichWithCommentsCount(post)
                .flatMap(response -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(response, "Operación exitosa")));
    }

    private Mono<PostResponse> enrichWithCommentsCount(Post post) {
        return commentUseCase.countByPostId(post.getId())
                .defaultIfEmpty(0L)
                .map(count -> toResponse(post, count));
    }

    private PostResponse toResponse(Post post, Long commentsCount) {
        return PostResponse.builder()
                .id(post.getId())
                .organizationId(post.getOrganizationId())
                .authorId(post.getAuthorId())
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType() != null ? post.getType().name() : null)
                .allowComments(post.getAllowComments())
                .isPinned(post.getIsPinned())
                .status(post.getStatus() != null ? post.getStatus().name() : null)
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .commentsCount(commentsCount)
                .build();
    }
}
