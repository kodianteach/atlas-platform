package co.com.atlas.api.poll;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.poll.dto.PollRequest;
import co.com.atlas.api.poll.dto.PollResponse;
import co.com.atlas.api.poll.dto.VoteRequest;
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
@Tag(name = "Polls", description = "Gestión de encuestas y votaciones")
public class PollRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/polls",
                    method = RequestMethod.POST,
                    beanClass = PollHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createPoll",
                            summary = "Crear encuesta",
                            description = "Crea una nueva encuesta con sus opciones (requiere header X-User-Id)",
                            tags = {"Polls"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = PollRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Encuesta creada",
                                            content = @Content(schema = @Schema(implementation = PollResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Debe tener al menos 2 opciones",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/polls/{id}",
                    method = RequestMethod.GET,
                    beanClass = PollHandler.class,
                    beanMethod = "findById",
                    operation = @Operation(
                            operationId = "getPollById",
                            summary = "Obtener encuesta por ID",
                            description = "Obtiene una encuesta con sus opciones y conteo de votos",
                            tags = {"Polls"},
                            parameters = @Parameter(name = "id", description = "ID de la encuesta", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Encuesta encontrada",
                                            content = @Content(schema = @Schema(implementation = PollResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Encuesta no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/polls/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = PollHandler.class,
                    beanMethod = "findByOrganizationId",
                    operation = @Operation(
                            operationId = "getPollsByOrganization",
                            summary = "Listar encuestas por organización",
                            tags = {"Polls"},
                            parameters = @Parameter(name = "organizationId", description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de encuestas")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/polls/organization/{organizationId}/active",
                    method = RequestMethod.GET,
                    beanClass = PollHandler.class,
                    beanMethod = "findActive",
                    operation = @Operation(
                            operationId = "getActivePollsByOrganization",
                            summary = "Listar encuestas activas",
                            tags = {"Polls"},
                            parameters = @Parameter(name = "organizationId", description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de encuestas activas")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/polls/{id}/activate",
                    method = RequestMethod.POST,
                    beanClass = PollHandler.class,
                    beanMethod = "activate",
                    operation = @Operation(
                            operationId = "activatePoll",
                            summary = "Activar encuesta",
                            description = "Cambia el estado de la encuesta a ACTIVE",
                            tags = {"Polls"},
                            parameters = @Parameter(name = "id", description = "ID de la encuesta", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Encuesta activada"),
                                    @ApiResponse(responseCode = "404", description = "Encuesta no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/polls/{id}/close",
                    method = RequestMethod.POST,
                    beanClass = PollHandler.class,
                    beanMethod = "close",
                    operation = @Operation(
                            operationId = "closePoll",
                            summary = "Cerrar encuesta",
                            description = "Cambia el estado de la encuesta a CLOSED",
                            tags = {"Polls"},
                            parameters = @Parameter(name = "id", description = "ID de la encuesta", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Encuesta cerrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/polls/{id}/vote",
                    method = RequestMethod.POST,
                    beanClass = PollHandler.class,
                    beanMethod = "vote",
                    operation = @Operation(
                            operationId = "voteOnPoll",
                            summary = "Votar en encuesta",
                            description = "Registra un voto en una encuesta activa (requiere header X-User-Id)",
                            tags = {"Polls"},
                            parameters = @Parameter(name = "id", description = "ID de la encuesta", required = true),
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = VoteRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Voto registrado"),
                                    @ApiResponse(responseCode = "400", description = "Encuesta no activa o ya votó",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/polls/{id}/results",
                    method = RequestMethod.GET,
                    beanClass = PollHandler.class,
                    beanMethod = "getResults",
                    operation = @Operation(
                            operationId = "getPollResults",
                            summary = "Obtener resultados de encuesta",
                            description = "Obtiene los resultados con conteo de votos y porcentajes",
                            tags = {"Polls"},
                            parameters = @Parameter(name = "id", description = "ID de la encuesta", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Resultados de la encuesta",
                                            content = @Content(schema = @Schema(implementation = PollResponse.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> pollRoutes(PollHandler handler) {
        return route(POST("/api/polls"), handler::create)
                .andRoute(GET("/api/polls/{id}"), handler::findById)
                .andRoute(GET("/api/polls/organization/{organizationId}"), handler::findByOrganizationId)
                .andRoute(GET("/api/polls/organization/{organizationId}/active"), handler::findActive)
                .andRoute(POST("/api/polls/{id}/activate"), handler::activate)
                .andRoute(POST("/api/polls/{id}/close"), handler::close)
                .andRoute(POST("/api/polls/{id}/vote"), handler::vote)
                .andRoute(GET("/api/polls/{id}/results"), handler::getResults);
    }
}
