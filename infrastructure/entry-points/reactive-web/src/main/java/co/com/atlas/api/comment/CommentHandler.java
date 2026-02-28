package co.com.atlas.api.comment;

import co.com.atlas.api.comment.dto.CommentRequest;
import co.com.atlas.api.comment.dto.CommentResponse;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.comment.Comment;
import co.com.atlas.usecase.comment.CommentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CommentHandler {

    private final CommentUseCase commentUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        Long authorId = Long.valueOf(request.headers().firstHeader("X-User-Id"));
        String authorRole = request.headers().firstHeader("X-User-Role");
        
        return request.bodyToMono(CommentRequest.class)
                .map(req -> Comment.builder()
                        .postId(req.getPostId())
                        .authorId(authorId)
                        .parentId(req.getParentId())
                        .content(req.getContent())
                        .authorRole(authorRole)
                        .build())
                .flatMap(commentUseCase::create)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return commentUseCase.findById(id)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> findByPostId(ServerRequest request) {
        Long postId = Long.valueOf(request.pathVariable("postId"));
        return commentUseCase.findByPostId(postId)
                .map(this::toResponse)
                .collectList()
                .flatMap(comments -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(comments, "Comentarios obtenidos exitosamente")));
    }

    public Mono<ServerResponse> findReplies(ServerRequest request) {
        Long parentId = Long.valueOf(request.pathVariable("parentId"));
        return commentUseCase.findReplies(parentId)
                .map(this::toResponse)
                .collectList()
                .flatMap(replies -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(replies, "Respuestas obtenidas exitosamente")));
    }

    public Mono<ServerResponse> countByPostId(ServerRequest request) {
        Long postId = Long.valueOf(request.pathVariable("postId"));
        return commentUseCase.countByPostId(postId)
                .flatMap(count -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(count, "Conteo obtenido exitosamente")));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return commentUseCase.delete(id)
                .then(ServerResponse.ok().bodyValue(ApiResponse.success(null, "Comentario eliminado exitosamente")));
    }

    /**
     * Oculta un comentario (marca como no aprobado).
     */
    public Mono<ServerResponse> hideComment(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return commentUseCase.hideComment(id)
                .flatMap(this::buildSuccessResponse);
    }

    /**
     * Aprueba un comentario flaggeado (falso positivo).
     */
    public Mono<ServerResponse> approveComment(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return commentUseCase.approveComment(id)
                .flatMap(this::buildSuccessResponse);
    }

    /**
     * Obtiene comentarios flaggeados por moderación de una organización.
     */
    public Mono<ServerResponse> getFlaggedComments(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return commentUseCase.findFlaggedByOrganization(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(comments -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(comments, "Comentarios flaggeados obtenidos")));
    }

    /**
     * Obtiene todos los comentarios de un post incluyendo ocultos (vista admin).
     */
    public Mono<ServerResponse> getAllCommentsByPost(ServerRequest request) {
        Long postId = Long.valueOf(request.pathVariable("postId"));
        return commentUseCase.findAllByPostIdAdmin(postId)
                .map(this::toResponse)
                .collectList()
                .flatMap(comments -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(comments, "Comentarios obtenidos")));
    }

    private Mono<ServerResponse> buildSuccessResponse(Comment comment) {
        return ServerResponse.ok()
                .bodyValue(ApiResponse.success(toResponse(comment), "Operación exitosa"));
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .isApproved(comment.getIsApproved())
                .flagReason(comment.getFlagReason())
                .authorRole(comment.getAuthorRole())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
