package co.com.atlas.api.tower;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.tower.dto.TowerRequest;
import co.com.atlas.api.tower.dto.TowerResponse;
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
 * Router para endpoints de Tower.
 */
@Configuration
@Tag(name = "Towers", description = "Gestión de torres")
public class TowerRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/towers",
                    method = RequestMethod.POST,
                    beanClass = TowerHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createTower",
                            summary = "Crear torre",
                            tags = {"Towers"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = TowerRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Torre creada",
                                            content = @Content(schema = @Schema(implementation = TowerResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/towers/{id}",
                    method = RequestMethod.GET,
                    beanClass = TowerHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getTowerById",
                            summary = "Obtener torre por ID",
                            tags = {"Towers"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la torre", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Torre encontrada"),
                                    @ApiResponse(responseCode = "404", description = "Torre no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/towers/zone/{zoneId}",
                    method = RequestMethod.GET,
                    beanClass = TowerHandler.class,
                    beanMethod = "getByZone",
                    operation = @Operation(
                            operationId = "getTowersByZone",
                            summary = "Listar torres por zona",
                            tags = {"Towers"},
                            parameters = @Parameter(name = "zoneId", in = ParameterIn.PATH, description = "ID de la zona", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de torres")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/towers/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = TowerHandler.class,
                    beanMethod = "getByOrganization",
                    operation = @Operation(
                            operationId = "getTowersByOrganization",
                            summary = "Listar torres por organización",
                            tags = {"Towers"},
                            parameters = @Parameter(name = "organizationId", in = ParameterIn.PATH, description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de torres")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/towers/{id}",
                    method = RequestMethod.PUT,
                    beanClass = TowerHandler.class,
                    beanMethod = "update",
                    operation = @Operation(
                            operationId = "updateTower",
                            summary = "Actualizar torre",
                            tags = {"Towers"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la torre", required = true),
                            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = TowerRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Torre actualizada"),
                                    @ApiResponse(responseCode = "404", description = "Torre no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/towers/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = TowerHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteTower",
                            summary = "Eliminar torre",
                            tags = {"Towers"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la torre", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Torre eliminada")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> towerRoutes(TowerHandler handler) {
        return route(POST("/api/towers").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/towers/{id}"), handler::getById)
                .andRoute(GET("/api/towers/zone/{zoneId}"), handler::getByZone)
                .andRoute(GET("/api/towers/organization/{organizationId}"), handler::getByOrganization)
                .andRoute(PUT("/api/towers/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::update)
                .andRoute(DELETE("/api/towers/{id}"), handler::delete);
    }
}
