package co.com.atlas.api.visit;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.visit.dto.VisitApprovalDto;
import co.com.atlas.api.visit.dto.VisitRequestDto;
import co.com.atlas.api.visit.dto.VisitRequestResponse;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints de VisitRequest.
 */
@Configuration
@Tag(name = "Visits", description = "Gestión de solicitudes de visita")
public class VisitRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/visits",
                    method = RequestMethod.POST,
                    beanClass = VisitHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createVisitRequest",
                            summary = "Crear solicitud de visita",
                            tags = {"Visits"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = VisitRequestDto.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Solicitud creada",
                                            content = @Content(schema = @Schema(implementation = VisitRequestResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/visits/{id}",
                    method = RequestMethod.GET,
                    beanClass = VisitHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getVisitById",
                            summary = "Obtener solicitud por ID",
                            tags = {"Visits"},
                            parameters = @Parameter(name = "id", description = "ID de la solicitud", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
                                    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/visits/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = VisitHandler.class,
                    beanMethod = "getByOrganization",
                    operation = @Operation(
                            operationId = "getVisitsByOrganization",
                            summary = "Listar solicitudes por organización",
                            tags = {"Visits"},
                            parameters = @Parameter(name = "organizationId", description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de solicitudes")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/visits/unit/{unitId}",
                    method = RequestMethod.GET,
                    beanClass = VisitHandler.class,
                    beanMethod = "getByUnit",
                    operation = @Operation(
                            operationId = "getVisitsByUnit",
                            summary = "Listar solicitudes por unidad",
                            tags = {"Visits"},
                            parameters = @Parameter(name = "unitId", description = "ID de la unidad", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de solicitudes")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/visits/organization/{organizationId}/pending",
                    method = RequestMethod.GET,
                    beanClass = VisitHandler.class,
                    beanMethod = "getPendingByOrganization",
                    operation = @Operation(
                            operationId = "getPendingVisitsByOrganization",
                            summary = "Listar solicitudes pendientes",
                            tags = {"Visits"},
                            parameters = @Parameter(name = "organizationId", description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de solicitudes pendientes")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/visits/{id}/approve",
                    method = RequestMethod.POST,
                    beanClass = VisitHandler.class,
                    beanMethod = "approve",
                    operation = @Operation(
                            operationId = "approveVisit",
                            summary = "Aprobar solicitud de visita",
                            description = "Aprueba la solicitud y genera código de acceso",
                            tags = {"Visits"},
                            parameters = @Parameter(name = "id", description = "ID de la solicitud", required = true),
                            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = VisitApprovalDto.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Solicitud aprobada"),
                                    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/visits/{id}/reject",
                    method = RequestMethod.POST,
                    beanClass = VisitHandler.class,
                    beanMethod = "reject",
                    operation = @Operation(
                            operationId = "rejectVisit",
                            summary = "Rechazar solicitud de visita",
                            tags = {"Visits"},
                            parameters = @Parameter(name = "id", description = "ID de la solicitud", required = true),
                            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = VisitApprovalDto.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Solicitud rechazada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/visits/{id}/cancel",
                    method = RequestMethod.POST,
                    beanClass = VisitHandler.class,
                    beanMethod = "cancel",
                    operation = @Operation(
                            operationId = "cancelVisit",
                            summary = "Cancelar solicitud de visita",
                            tags = {"Visits"},
                            parameters = @Parameter(name = "id", description = "ID de la solicitud", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Solicitud cancelada")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> visitRoutes(VisitHandler handler) {
        return route(POST("/api/visits").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/visits/{id}"), handler::getById)
                .andRoute(GET("/api/visits/organization/{organizationId}"), handler::getByOrganization)
                .andRoute(GET("/api/visits/unit/{unitId}"), handler::getByUnit)
                .andRoute(GET("/api/visits/organization/{organizationId}/pending"), handler::getPendingByOrganization)
                .andRoute(POST("/api/visits/{id}/approve").and(accept(MediaType.APPLICATION_JSON)), handler::approve)
                .andRoute(POST("/api/visits/{id}/reject").and(accept(MediaType.APPLICATION_JSON)), handler::reject)
                .andRoute(POST("/api/visits/{id}/cancel"), handler::cancel);
    }
}
