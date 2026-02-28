package co.com.atlas.api.post;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.post.dto.PostRequest;
import co.com.atlas.api.post.dto.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
@Tag(name = "Posts", description = "Gestión de publicaciones y anuncios")
public class PostRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/posts",
                    method = RequestMethod.POST,
                    beanClass = PostHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createPost",
                            summary = "Crear publicación",
                            description = "Crea una nueva publicación (requiere header X-User-Id)",
                            tags = {"Posts"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = PostRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Post creado",
                                            content = @Content(schema = @Schema(implementation = PostResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/{id}",
                    method = RequestMethod.GET,
                    beanClass = PostHandler.class,
                    beanMethod = "findById",
                    operation = @Operation(
                            operationId = "getPostById",
                            summary = "Obtener post por ID",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del post", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Post encontrado",
                                            content = @Content(schema = @Schema(implementation = PostResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Post no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/{id}",
                    method = RequestMethod.PUT,
                    beanClass = PostHandler.class,
                    beanMethod = "update",
                    operation = @Operation(
                            operationId = "updatePost",
                            summary = "Actualizar post",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del post", required = true),
                            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = PostRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Post actualizado"),
                                    @ApiResponse(responseCode = "404", description = "Post no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = PostHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deletePost",
                            summary = "Eliminar post",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del post", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Post eliminado"),
                                    @ApiResponse(responseCode = "404", description = "Post no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = PostHandler.class,
                    beanMethod = "findByOrganizationId",
                    operation = @Operation(
                            operationId = "getPostsByOrganization",
                            summary = "Listar posts por organización",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "organizationId", in = ParameterIn.PATH, description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de posts")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/organization/{organizationId}/published",
                    method = RequestMethod.GET,
                    beanClass = PostHandler.class,
                    beanMethod = "findPublished",
                    operation = @Operation(
                            operationId = "getPublishedPosts",
                            summary = "Listar posts publicados",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "organizationId", in = ParameterIn.PATH, description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de posts publicados")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/{id}/publish",
                    method = RequestMethod.POST,
                    beanClass = PostHandler.class,
                    beanMethod = "publish",
                    operation = @Operation(
                            operationId = "publishPost",
                            summary = "Publicar post",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del post", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Post publicado"),
                                    @ApiResponse(responseCode = "404", description = "Post no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/{id}/archive",
                    method = RequestMethod.POST,
                    beanClass = PostHandler.class,
                    beanMethod = "archive",
                    operation = @Operation(
                            operationId = "archivePost",
                            summary = "Archivar post",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del post", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Post archivado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/posts/{id}/toggle-pin",
                    method = RequestMethod.POST,
                    beanClass = PostHandler.class,
                    beanMethod = "togglePin",
                    operation = @Operation(
                            operationId = "togglePinPost",
                            summary = "Fijar/Desfijar post",
                            tags = {"Posts"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del post", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Estado de fijado cambiado")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> postRoutes(PostHandler handler) {
        return route(POST("/api/posts"), handler::create)
                .andRoute(GET("/api/posts/admin/search"), handler::searchPosts)
                .andRoute(GET("/api/posts/admin/stats"), handler::getPostStats)
                .andRoute(GET("/api/posts/{id}"), handler::findById)
                .andRoute(PUT("/api/posts/{id}"), handler::update)
                .andRoute(DELETE("/api/posts/{id}"), handler::delete)
                .andRoute(GET("/api/posts/organization/{organizationId}"), handler::findByOrganizationId)
                .andRoute(GET("/api/posts/organization/{organizationId}/published"), handler::findPublished)
                .andRoute(POST("/api/posts/{id}/publish"), handler::publish)
                .andRoute(POST("/api/posts/{id}/archive"), handler::archive)
                .andRoute(POST("/api/posts/{id}/toggle-pin"), handler::togglePin)
                .andRoute(POST("/api/posts/{id}/reactivate"), handler::reactivatePost);
    }
}
