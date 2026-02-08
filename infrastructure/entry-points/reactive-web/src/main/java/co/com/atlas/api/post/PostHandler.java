package co.com.atlas.api.post;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.post.dto.PostRequest;
import co.com.atlas.api.post.dto.PostResponse;
import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostType;
import co.com.atlas.usecase.post.PostUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PostHandler {

    private final PostUseCase postUseCase;

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
                .map(this::toResponse)
                .collectList()
                .flatMap(posts -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(posts, "Posts obtenidos exitosamente")));
    }

    public Mono<ServerResponse> findPublished(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return postUseCase.findPublishedByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(posts -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(posts, "Posts publicados obtenidos exitosamente")));
    }

    public Mono<ServerResponse> publish(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return postUseCase.publish(id)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> archive(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return postUseCase.archive(id)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> togglePin(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return postUseCase.togglePin(id)
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
        return postUseCase.delete(id)
                .then(ServerResponse.ok().bodyValue(ApiResponse.success(null, "Post eliminado exitosamente")));
    }

    private Mono<ServerResponse> buildSuccessResponse(Post post) {
        return ServerResponse.ok()
                .bodyValue(ApiResponse.success(toResponse(post), "Operación exitosa"));
    }

    private PostResponse toResponse(Post post) {
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
                .build();
    }
}
