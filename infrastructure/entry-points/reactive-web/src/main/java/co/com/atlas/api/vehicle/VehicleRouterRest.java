package co.com.atlas.api.vehicle;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.vehicle.dto.*;
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
 * Router para endpoints de Vehicle.
 */
@Configuration
@Tag(name = "Vehicles", description = "Gestión y validación de vehículos por vivienda")
public class VehicleRouterRest {

    @Bean
    @RouterOperations({
            // ========== CRUD ==========
            @RouterOperation(
                    path = "/api/vehicles",
                    method = RequestMethod.POST,
                    beanClass = VehicleHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createVehicle",
                            summary = "Registrar vehículo",
                            tags = {"Vehicles"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = VehicleRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Vehículo registrado",
                                            content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "409", description = "Placa duplicada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/vehicles/{id}",
                    method = RequestMethod.GET,
                    beanClass = VehicleHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getVehicleById",
                            summary = "Obtener vehículo por ID",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del vehículo", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Vehículo encontrado"),
                                    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/vehicles/unit/{unitId}",
                    method = RequestMethod.GET,
                    beanClass = VehicleHandler.class,
                    beanMethod = "getByUnit",
                    operation = @Operation(
                            operationId = "getVehiclesByUnit",
                            summary = "Listar vehículos de una unidad",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "unitId", in = ParameterIn.PATH, description = "ID de la unidad", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de vehículos")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/vehicles/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = VehicleHandler.class,
                    beanMethod = "getByOrganization",
                    operation = @Operation(
                            operationId = "getVehiclesByOrganization",
                            summary = "Listar vehículos de una organización (paginado)",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "organizationId", in = ParameterIn.PATH, description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Página de vehículos")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/vehicles/{id}",
                    method = RequestMethod.PUT,
                    beanClass = VehicleHandler.class,
                    beanMethod = "update",
                    operation = @Operation(
                            operationId = "updateVehicle",
                            summary = "Actualizar vehículo",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del vehículo", required = true),
                            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = VehicleRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Vehículo actualizado"),
                                    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado"),
                                    @ApiResponse(responseCode = "409", description = "Placa duplicada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/vehicles/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = VehicleHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteVehicle",
                            summary = "Eliminar vehículo (soft delete)",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del vehículo", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Vehículo eliminado")
                            }
                    )
            ),
            // ========== Guard / Validate ==========
            @RouterOperation(
                    path = "/api/vehicles/validate/{plate}",
                    method = RequestMethod.GET,
                    beanClass = VehicleHandler.class,
                    beanMethod = "validatePlate",
                    operation = @Operation(
                            operationId = "validateVehiclePlate",
                            summary = "Validar placa de vehículo (API de guarda)",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "plate", in = ParameterIn.PATH, description = "Placa del vehículo", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Resultado de validación",
                                            content = @Content(schema = @Schema(implementation = PlateValidationResponse.class)))
                            }
                    )
            ),
            // ========== Bulk Operations ==========
            @RouterOperation(
                    path = "/api/vehicles/unit/{unitId}/inactivate",
                    method = RequestMethod.PATCH,
                    beanClass = VehicleHandler.class,
                    beanMethod = "bulkInactivate",
                    operation = @Operation(
                            operationId = "bulkInactivateVehicles",
                            summary = "Inactivar todos los vehículos de una unidad",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "unitId", in = ParameterIn.PATH, description = "ID de la unidad", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Vehículos inactivados",
                                            content = @Content(schema = @Schema(implementation = BulkInactivateResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Unidad no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/vehicles/unit/{unitId}/sync",
                    method = RequestMethod.PUT,
                    beanClass = VehicleHandler.class,
                    beanMethod = "bulkSync",
                    operation = @Operation(
                            operationId = "bulkSyncVehicles",
                            summary = "Sincronizar vehículos de una unidad (bulk)",
                            tags = {"Vehicles"},
                            parameters = @Parameter(name = "unitId", in = ParameterIn.PATH, description = "ID de la unidad", required = true),
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = BulkSyncRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Sincronización completa",
                                            content = @Content(schema = @Schema(implementation = BulkSyncResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Límite de vehículos excedido"),
                                    @ApiResponse(responseCode = "404", description = "Unidad no encontrada")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> vehicleRoutes(VehicleHandler handler) {
        return route(POST("/api/vehicles").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/vehicles/validate/{plate}"), handler::validatePlate)
                .andRoute(GET("/api/vehicles/unit/{unitId}"), handler::getByUnit)
                .andRoute(GET("/api/vehicles/organization/{organizationId}"), handler::getByOrganization)
                .andRoute(GET("/api/vehicles/{id}"), handler::getById)
                .andRoute(PUT("/api/vehicles/unit/{unitId}/sync").and(accept(MediaType.APPLICATION_JSON)), handler::bulkSync)
                .andRoute(PUT("/api/vehicles/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::update)
                .andRoute(PATCH("/api/vehicles/unit/{unitId}/inactivate"), handler::bulkInactivate)
                .andRoute(DELETE("/api/vehicles/{id}"), handler::delete);
    }
}
