package co.com.atlas.api.organization;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.organization.dto.OrganizationSettingsDto;
import co.com.atlas.api.organization.dto.OrganizationSettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
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
 * Router para configuración de organización.
 */
@Configuration
@Tag(name = "Organization Settings", description = "Configuración de organización")
public class OrganizationSettingsRouterRest {

    @Bean("organizationSettingsRoutes")
    @RouterOperations({
        @RouterOperation(
            path = "/api/organization/settings",
            method = RequestMethod.GET,
            beanClass = OrganizationSettingsHandler.class,
            beanMethod = "getSettings",
            operation = @Operation(
                operationId = "getOrganizationSettings",
                summary = "Obtener configuración de organización",
                description = "Obtiene los parámetros configurables de la organización del usuario autenticado",
                tags = {"Organization Settings"},
                responses = {
                    @ApiResponse(responseCode = "200", description = "Configuración obtenida",
                        content = @Content(schema = @Schema(implementation = OrganizationSettingsResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Error de validación",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                }
            )
        ),
        @RouterOperation(
            path = "/api/organization/settings",
            method = RequestMethod.PUT,
            beanClass = OrganizationSettingsHandler.class,
            beanMethod = "updateSettings",
            operation = @Operation(
                operationId = "updateOrganizationSettings",
                summary = "Actualizar configuración de organización",
                description = "Actualiza los parámetros configurables de la organización del usuario autenticado",
                tags = {"Organization Settings"},
                requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = OrganizationSettingsDto.class))
                ),
                responses = {
                    @ApiResponse(responseCode = "200", description = "Configuración actualizada",
                        content = @Content(schema = @Schema(implementation = OrganizationSettingsResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Error de validación",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                }
            )
        )
    })
    public RouterFunction<ServerResponse> organizationSettingsRouterFunction(
            OrganizationSettingsHandler handler) {
        return route(GET("/api/organization/settings").and(accept(MediaType.APPLICATION_JSON)), handler::getSettings)
            .andRoute(PUT("/api/organization/settings").and(accept(MediaType.APPLICATION_JSON)), handler::updateSettings);
    }
}
