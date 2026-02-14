package co.com.atlas.api.unit;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.unit.dto.UnitRequest;
import co.com.atlas.api.unit.dto.UnitResponse;
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
 * Router para endpoints de Unit.
 */
@Configuration
@Tag(name = "Units", description = "Gestión de unidades (apartamentos/casas)")
public class UnitRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/units",
                    method = RequestMethod.POST,
                    beanClass = UnitHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createUnit",
                            summary = "Crear unidad",
                            tags = {"Units"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = UnitRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Unidad creada",
                                            content = @Content(schema = @Schema(implementation = UnitResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/{id}",
                    method = RequestMethod.GET,
                    beanClass = UnitHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getUnitById",
                            summary = "Obtener unidad por ID",
                            tags = {"Units"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la unidad", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Unidad encontrada"),
                                    @ApiResponse(responseCode = "404", description = "Unidad no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = UnitHandler.class,
                    beanMethod = "getByOrganization",
                    operation = @Operation(
                            operationId = "getUnitsByOrganization",
                            summary = "Listar unidades por organización",
                            tags = {"Units"},
                            parameters = @Parameter(name = "organizationId", in = ParameterIn.PATH, description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de unidades")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/tower/{towerId}",
                    method = RequestMethod.GET,
                    beanClass = UnitHandler.class,
                    beanMethod = "getByTower",
                    operation = @Operation(
                            operationId = "getUnitsByTower",
                            summary = "Listar unidades por torre",
                            tags = {"Units"},
                            parameters = @Parameter(name = "towerId", in = ParameterIn.PATH, description = "ID de la torre", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de unidades")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/{id}",
                    method = RequestMethod.PUT,
                    beanClass = UnitHandler.class,
                    beanMethod = "update",
                    operation = @Operation(
                            operationId = "updateUnit",
                            summary = "Actualizar unidad",
                            tags = {"Units"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la unidad", required = true),
                            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = UnitRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Unidad actualizada"),
                                    @ApiResponse(responseCode = "404", description = "Unidad no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = UnitHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteUnit",
                            summary = "Eliminar unidad",
                            tags = {"Units"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la unidad", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Unidad eliminada")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> unitRoutes(UnitHandler handler) {
        return route(POST("/api/units").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/units/{id}"), handler::getById)
                .andRoute(GET("/api/units/organization/{organizationId}"), handler::getByOrganization)
                .andRoute(GET("/api/units/tower/{towerId}"), handler::getByTower)
                .andRoute(PUT("/api/units/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::update)
                .andRoute(DELETE("/api/units/{id}"), handler::delete);
    }
}
