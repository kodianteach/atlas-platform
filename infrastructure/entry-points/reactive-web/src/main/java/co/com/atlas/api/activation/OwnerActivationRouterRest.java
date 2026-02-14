package co.com.atlas.api.activation;

import co.com.atlas.api.activation.dto.CompleteOwnerActivationRequest;
import co.com.atlas.api.activation.dto.CompleteOwnerActivationResponse;
import co.com.atlas.api.activation.dto.ValidateOwnerTokenResponse;
import co.com.atlas.api.common.dto.ErrorResponse;
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
 * Router para endpoints de activación de propietarios.
 * Estos endpoints son PÚBLICOS (no requieren autenticación).
 */
@Configuration
@Tag(name = "Owner Activation", description = "Activación de propietarios invitados")
public class OwnerActivationRouterRest {

    @Bean("ownerActivationRoutes")
    @RouterOperations({
            @RouterOperation(
                    path = "/api/activation/owner/validate/{token}",
                    method = RequestMethod.GET,
                    beanClass = OwnerActivationHandler.class,
                    beanMethod = "validateToken",
                    operation = @Operation(
                            operationId = "validateOwnerToken",
                            summary = "Validar token de invitación",
                            description = "Valida un token de invitación de propietario y retorna información de la invitación",
                            tags = {"Owner Activation"},
                            parameters = {
                                    @Parameter(name = "token", in = ParameterIn.PATH, description = "Token de invitación", required = true)
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Validación exitosa",
                                            content = @Content(schema = @Schema(implementation = ValidateOwnerTokenResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Token no encontrado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Token inválido o expirado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/activation/owner/complete",
                    method = RequestMethod.POST,
                    beanClass = OwnerActivationHandler.class,
                    beanMethod = "completeActivation",
                    operation = @Operation(
                            operationId = "completeOwnerActivation",
                            summary = "Completar activación de propietario",
                            description = "Completa la activación del propietario estableciendo su contraseña",
                            tags = {"Owner Activation"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = CompleteOwnerActivationRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Activación completada",
                                            content = @Content(schema = @Schema(implementation = CompleteOwnerActivationResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(responseCode = "404", description = "Token no encontrado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> ownerActivationRoutes(OwnerActivationHandler handler) {
        return route(GET("/api/activation/owner/validate/{token}"), handler::validateToken)
                .andRoute(POST("/api/activation/owner/complete").and(accept(MediaType.APPLICATION_JSON)), 
                        handler::completeActivation);
    }
}
