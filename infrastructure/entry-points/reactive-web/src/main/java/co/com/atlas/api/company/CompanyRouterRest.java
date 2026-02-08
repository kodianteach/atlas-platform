package co.com.atlas.api.company;

import co.com.atlas.api.company.dto.CompanyRequest;
import co.com.atlas.api.company.dto.CompanyResponse;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints de Company.
 */
@Configuration
@Tag(name = "Companies", description = "Gestión de compañías holding")
public class CompanyRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/companies",
                    method = RequestMethod.POST,
                    beanClass = CompanyHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createCompany",
                            summary = "Crear compañía",
                            description = "Crea una nueva compañía holding",
                            tags = {"Companies"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = CompanyRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Compañía creada",
                                            content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/companies/{id}",
                    method = RequestMethod.GET,
                    beanClass = CompanyHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getCompanyById",
                            summary = "Obtener compañía por ID",
                            tags = {"Companies"},
                            parameters = @Parameter(name = "id", description = "ID de la compañía", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Compañía encontrada",
                                            content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Compañía no encontrada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/companies/slug/{slug}",
                    method = RequestMethod.GET,
                    beanClass = CompanyHandler.class,
                    beanMethod = "getBySlug",
                    operation = @Operation(
                            operationId = "getCompanyBySlug",
                            summary = "Obtener compañía por slug",
                            tags = {"Companies"},
                            parameters = @Parameter(name = "slug", description = "Slug de la compañía", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Compañía encontrada",
                                            content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Compañía no encontrada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/companies",
                    method = RequestMethod.GET,
                    beanClass = CompanyHandler.class,
                    beanMethod = "getAll",
                    operation = @Operation(
                            operationId = "getAllCompanies",
                            summary = "Listar todas las compañías activas",
                            tags = {"Companies"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de compañías")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/companies/{id}",
                    method = RequestMethod.PUT,
                    beanClass = CompanyHandler.class,
                    beanMethod = "update",
                    operation = @Operation(
                            operationId = "updateCompany",
                            summary = "Actualizar compañía",
                            tags = {"Companies"},
                            parameters = @Parameter(name = "id", description = "ID de la compañía", required = true),
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = CompanyRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Compañía actualizada",
                                            content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Compañía no encontrada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/companies/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = CompanyHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteCompany",
                            summary = "Eliminar compañía",
                            tags = {"Companies"},
                            parameters = @Parameter(name = "id", description = "ID de la compañía", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Compañía eliminada"),
                                    @ApiResponse(responseCode = "404", description = "Compañía no encontrada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> companyRoutes(CompanyHandler handler) {
        return route(POST("/api/companies").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/companies/{id}"), handler::getById)
                .andRoute(GET("/api/companies/slug/{slug}"), handler::getBySlug)
                .andRoute(GET("/api/companies"), handler::getAll)
                .andRoute(PUT("/api/companies/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::update)
                .andRoute(DELETE("/api/companies/{id}"), handler::delete);
    }
}
