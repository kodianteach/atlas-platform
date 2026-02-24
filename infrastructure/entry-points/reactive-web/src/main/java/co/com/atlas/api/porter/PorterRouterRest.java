package co.com.atlas.api.porter;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.porter.dto.CreatePorterRequest;
import co.com.atlas.api.porter.dto.PorterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints de gestión de porteros.
 *
 * Endpoints:
 * - POST /api/porters → Crear portero
 * - GET /api/porters → Listar porteros de la organización
 * - POST /api/porters/{id}/regenerate-url → Regenerar URL de enrolamiento
 */
@Configuration
@Tag(name = "Porteros", description = "API para gestión de porteros (gatekeepers) del conjunto residencial")
public class PorterRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/porters",
                    method = RequestMethod.POST,
                    beanClass = PorterHandler.class,
                    beanMethod = "createPorter",
                    operation = @Operation(
                            operationId = "createPorter",
                            summary = "Crear portero",
                            description = """
                                    Crea un nuevo portero para la organización actual.
                                    
                                    El sistema:
                                    - Crea un usuario sintético con estado PRE_REGISTERED
                                    - Asigna el rol correspondiente (PORTERO_GENERAL o PORTERO_DELIVERY)
                                    - Genera un token de enrolamiento con vigencia de 24 horas
                                    - Registra eventos de auditoría
                                    
                                    La URL de enrolamiento devuelta debe ser compartida
                                    con el portero para que configure su dispositivo.
                                    """,
                            tags = {"Porteros"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = CreatePorterRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Portero creado exitosamente",
                                            content = @Content(schema = @Schema(implementation = PorterResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Datos de entrada inválidos",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/porters",
                    method = RequestMethod.GET,
                    beanClass = PorterHandler.class,
                    beanMethod = "listPorters",
                    operation = @Operation(
                            operationId = "listPorters",
                            summary = "Listar porteros",
                            description = """
                                    Lista todos los porteros de la organización actual del administrador.
                                    
                                    Los porteros son filtrados por la organización del token JWT.
                                    """,
                            tags = {"Porteros"},
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Lista de porteros",
                                            content = @Content(array = @ArraySchema(
                                                    schema = @Schema(implementation = PorterResponse.class)))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/porters/{id}/regenerate-url",
                    method = RequestMethod.POST,
                    beanClass = PorterHandler.class,
                    beanMethod = "regenerateEnrollmentUrl",
                    operation = @Operation(
                            operationId = "regeneratePorterEnrollmentUrl",
                            summary = "Regenerar URL de enrolamiento",
                            description = """
                                    Regenera la URL de enrolamiento para un portero existente.
                                    
                                    El sistema:
                                    - Revoca el token activo anterior (si existe)
                                    - Genera un nuevo token con vigencia de 24 horas
                                    - Registra la acción en auditoría
                                    
                                    Usar cuando el token anterior haya expirado o se necesite
                                    vincular el portero a un nuevo dispositivo.
                                    """,
                            tags = {"Porteros"},
                            parameters = {
                                    @Parameter(
                                            name = "id",
                                            description = "ID del portero",
                                            required = true,
                                            in = ParameterIn.PATH
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "URL regenerada exitosamente"
                                    ),
                                    @ApiResponse(
                                            responseCode = "404",
                                            description = "Portero no encontrado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> porterRoutes(PorterHandler handler) {
        return route(POST("/api/porters").and(accept(MediaType.APPLICATION_JSON)), handler::createPorter)
                .andRoute(GET("/api/porters"), handler::listPorters)
                .andRoute(POST("/api/porters/{id}/regenerate-url").and(accept(MediaType.APPLICATION_JSON)), handler::regenerateEnrollmentUrl)
                .andRoute(PUT("/api/porters/{id}/toggle-status"), handler::togglePorterStatus);
    }
}
