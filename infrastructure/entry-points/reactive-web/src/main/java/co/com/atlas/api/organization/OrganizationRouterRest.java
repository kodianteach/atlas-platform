package co.com.atlas.api.organization;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.organization.dto.OrganizationRequest;
import co.com.atlas.api.organization.dto.OrganizationResponse;
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
 * Router para endpoints de Organization.
 */
@Configuration
@Tag(name = "Organizations", description = "Gestión de organizaciones (Ciudadelas/Conjuntos/Condominios)")
public class OrganizationRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/organizations",
                    method = RequestMethod.POST,
                    beanClass = OrganizationHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createOrganization",
                            summary = "Crear organización",
                            tags = {"Organizations"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = OrganizationRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Organización creada",
                                            content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/organizations/{id}",
                    method = RequestMethod.GET,
                    beanClass = OrganizationHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getOrganizationById",
                            summary = "Obtener organización por ID",
                            tags = {"Organizations"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Organización encontrada",
                                            content = @Content(schema = @Schema(implementation = OrganizationResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/organizations/company/{companyId}",
                    method = RequestMethod.GET,
                    beanClass = OrganizationHandler.class,
                    beanMethod = "getByCompany",
                    operation = @Operation(
                            operationId = "getOrganizationsByCompany",
                            summary = "Obtener organizaciones por compañía",
                            tags = {"Organizations"},
                            parameters = @Parameter(name = "companyId", in = ParameterIn.PATH, required = true)
                    )
            ),
            @RouterOperation(
                    path = "/api/organizations",
                    method = RequestMethod.GET,
                    beanClass = OrganizationHandler.class,
                    beanMethod = "getAll",
                    operation = @Operation(
                            operationId = "getAllOrganizations",
                            summary = "Listar todas las organizaciones activas",
                            tags = {"Organizations"}
                    )
            ),
            @RouterOperation(
                    path = "/api/organizations/{id}",
                    method = RequestMethod.PUT,
                    beanClass = OrganizationHandler.class,
                    beanMethod = "update",
                    operation = @Operation(
                            operationId = "updateOrganization",
                            summary = "Actualizar organización",
                            tags = {"Organizations"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, required = true),
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = OrganizationRequest.class))
                            )
                    )
            ),
            @RouterOperation(
                    path = "/api/organizations/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = OrganizationHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteOrganization",
                            summary = "Eliminar organización",
                            tags = {"Organizations"},
                            parameters = @Parameter(name = "id", in = ParameterIn.PATH, required = true)
                    )
            )
    })
    public RouterFunction<ServerResponse> organizationRoutes(OrganizationHandler handler) {
        return route(POST("/api/organizations").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/organizations/{id}"), handler::getById)
                .andRoute(GET("/api/organizations/company/{companyId}"), handler::getByCompany)
                .andRoute(GET("/api/organizations"), handler::getAll)
                .andRoute(PUT("/api/organizations/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::update)
                .andRoute(DELETE("/api/organizations/{id}"), handler::delete);
    }
}
