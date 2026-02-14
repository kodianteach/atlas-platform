package co.com.atlas.api.zone;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.zone.dto.ZoneRequest;
import co.com.atlas.api.zone.dto.ZoneResponse;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints de Zone.
 */
@Configuration
@Tag(name = "Zones", description = "Gestión de zonas")
public class ZoneRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/zones",
                    method = RequestMethod.POST,
                    beanClass = ZoneHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createZone",
                            summary = "Crear zona",
                            tags = {"Zones"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = ZoneRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Zona creada",
                                            content = @Content(schema = @Schema(implementation = ZoneResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/zones/{id}",
                    method = RequestMethod.GET,
                    beanClass = ZoneHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getZoneById",
                            summary = "Obtener zona por ID",
                            tags = {"Zones"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la zona", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Zona encontrada"),
                                    @ApiResponse(responseCode = "404", description = "Zona no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/zones/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = ZoneHandler.class,
                    beanMethod = "getByOrganization",
                    operation = @Operation(
                            operationId = "getZonesByOrganization",
                            summary = "Listar zonas por organización",
                            tags = {"Zones"},
                            parameters = @Parameter(name = "organizationId", in = ParameterIn.PATH, description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de zonas")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/zones/{id}",
                    method = RequestMethod.PUT,
                    beanClass = ZoneHandler.class,
                    beanMethod = "update",
                    operation = @Operation(
                            operationId = "updateZone",
                            summary = "Actualizar zona",
                            tags = {"Zones"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la zona", required = true),
                            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ZoneRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Zona actualizada"),
                                    @ApiResponse(responseCode = "404", description = "Zona no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/zones/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = ZoneHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteZone",
                            summary = "Eliminar zona",
                            tags = {"Zones"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la zona", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Zona eliminada")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> zoneRoutes(ZoneHandler handler) {
        return route(POST("/api/zones").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/zones/{id}"), handler::getById)
                .andRoute(GET("/api/zones/organization/{organizationId}"), handler::getByOrganization)
                .andRoute(PUT("/api/zones/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::update)
                .andRoute(DELETE("/api/zones/{id}"), handler::delete);
    }
}
