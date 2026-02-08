package co.com.atlas.api.access;

import co.com.atlas.api.access.dto.ValidateCodeRequest;
import co.com.atlas.api.access.dto.ValidateCodeResponse;
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
 * Router para endpoints de AccessCode.
 */
@Configuration
@Tag(name = "Access", description = "Validación y gestión de códigos de acceso")
public class AccessRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/access/validate",
                    method = RequestMethod.POST,
                    beanClass = AccessHandler.class,
                    beanMethod = "validateCode",
                    operation = @Operation(
                            operationId = "validateAccessCode",
                            summary = "Validar código de acceso",
                            description = "Valida un código QR/numérico en portería",
                            tags = {"Access"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = ValidateCodeRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Resultado de validación",
                                            content = @Content(schema = @Schema(implementation = ValidateCodeResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Código inválido",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/access/visit/{visitRequestId}",
                    method = RequestMethod.GET,
                    beanClass = AccessHandler.class,
                    beanMethod = "getByVisitRequest",
                    operation = @Operation(
                            operationId = "getAccessCodesByVisit",
                            summary = "Obtener códigos por solicitud de visita",
                            tags = {"Access"},
                            parameters = @Parameter(name = "visitRequestId", description = "ID de la solicitud de visita", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de códigos de acceso")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/access/{accessCodeId}/logs",
                    method = RequestMethod.GET,
                    beanClass = AccessHandler.class,
                    beanMethod = "getScanLogs",
                    operation = @Operation(
                            operationId = "getAccessScanLogs",
                            summary = "Obtener historial de escaneos",
                            tags = {"Access"},
                            parameters = @Parameter(name = "accessCodeId", description = "ID del código de acceso", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de escaneos")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/access/{id}/revoke",
                    method = RequestMethod.POST,
                    beanClass = AccessHandler.class,
                    beanMethod = "revokeCode",
                    operation = @Operation(
                            operationId = "revokeAccessCode",
                            summary = "Revocar código de acceso",
                            tags = {"Access"},
                            parameters = @Parameter(name = "id", description = "ID del código de acceso", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Código revocado")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> accessRoutes(AccessHandler handler) {
        return route(POST("/api/access/validate").and(accept(MediaType.APPLICATION_JSON)), handler::validateCode)
                .andRoute(GET("/api/access/visit/{visitRequestId}"), handler::getByVisitRequest)
                .andRoute(GET("/api/access/{accessCodeId}/logs"), handler::getScanLogs)
                .andRoute(POST("/api/access/{id}/revoke"), handler::revokeCode);
    }
}
