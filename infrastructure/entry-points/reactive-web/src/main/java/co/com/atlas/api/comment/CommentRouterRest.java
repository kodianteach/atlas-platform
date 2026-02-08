package co.com.atlas.api.comment;

import co.com.atlas.api.comment.dto.CommentRequest;
import co.com.atlas.api.comment.dto.CommentResponse;
import co.com.atlas.api.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@Tag(name = "Comments", description = "Gestión de comentarios en publicaciones")
public class CommentRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/comments",
                    method = RequestMethod.POST,
                    beanClass = CommentHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createComment",
                            summary = "Crear comentario",
                            description = "Crea un comentario en un post (requiere header X-User-Id)",
                            tags = {"Comments"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = CommentRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Comentario creado",
                                            content = @Content(schema = @Schema(implementation = CommentResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Post no permite comentarios",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/comments/{id}",
                    method = RequestMethod.GET,
                    beanClass = CommentHandler.class,
                    beanMethod = "findById",
                    operation = @Operation(
                            operationId = "getCommentById",
                            summary = "Obtener comentario por ID",
                            tags = {"Comments"},
                            parameters = @Parameter(name = "id", description = "ID del comentario", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Comentario encontrado"),
                                    @ApiResponse(responseCode = "404", description = "Comentario no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/comments/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = CommentHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteComment",
                            summary = "Eliminar comentario",
                            tags = {"Comments"},
                            parameters = @Parameter(name = "id", description = "ID del comentario", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Comentario eliminado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/comments/post/{postId}",
                    method = RequestMethod.GET,
                    beanClass = CommentHandler.class,
                    beanMethod = "findByPostId",
                    operation = @Operation(
                            operationId = "getCommentsByPost",
                            summary = "Listar comentarios de un post",
                            tags = {"Comments"},
                            parameters = @Parameter(name = "postId", description = "ID del post", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de comentarios")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/comments/post/{postId}/count",
                    method = RequestMethod.GET,
                    beanClass = CommentHandler.class,
                    beanMethod = "countByPostId",
                    operation = @Operation(
                            operationId = "countCommentsByPost",
                            summary = "Contar comentarios de un post",
                            tags = {"Comments"},
                            parameters = @Parameter(name = "postId", description = "ID del post", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Conteo de comentarios")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/comments/{parentId}/replies",
                    method = RequestMethod.GET,
                    beanClass = CommentHandler.class,
                    beanMethod = "findReplies",
                    operation = @Operation(
                            operationId = "getCommentReplies",
                            summary = "Obtener respuestas a un comentario",
                            tags = {"Comments"},
                            parameters = @Parameter(name = "parentId", description = "ID del comentario padre", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de respuestas")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> commentRoutes(CommentHandler handler) {
        return route(POST("/api/comments"), handler::create)
                .andRoute(GET("/api/comments/{id}"), handler::findById)
                .andRoute(DELETE("/api/comments/{id}"), handler::delete)
                .andRoute(GET("/api/comments/post/{postId}"), handler::findByPostId)
                .andRoute(GET("/api/comments/post/{postId}/count"), handler::countByPostId)
                .andRoute(GET("/api/comments/{parentId}/replies"), handler::findReplies);
    }
}
