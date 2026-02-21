package co.com.atlas.api.invitation;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.invitation.dto.OwnerRegistrationRequest;
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
 * Router for owner invitation endpoints.
 * Includes authenticated (admin) and external (self-register) routes.
 */
@Configuration
@Tag(name = "Owner Invitations", description = "Gestión de invitaciones de propietarios")
public class OwnerInvitationRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/invitations/owner",
                    method = RequestMethod.POST,
                    beanClass = OwnerInvitationHandler.class,
                    beanMethod = "createOwnerInvitation",
                    operation = @Operation(
                            operationId = "createOwnerInvitation",
                            summary = "Generar invitación de propietario",
                            description = "Genera un enlace de invitación para que un propietario se auto-registre. Solo ADMIN_ATLAS.",
                            tags = {"Owner Invitations"},
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Invitación creada"),
                                    @ApiResponse(responseCode = "404", description = "Organización no encontrada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/invitations/validate",
                    method = RequestMethod.GET,
                    beanClass = OwnerInvitationHandler.class,
                    beanMethod = "validateToken",
                    operation = @Operation(
                            operationId = "validateInvitationToken",
                            summary = "Validar token de invitación",
                            description = "Valida un token de invitación y retorna su estado. Endpoint externo sin autenticación.",
                            tags = {"Owner Invitations"},
                            parameters = @Parameter(name = "token", in = ParameterIn.QUERY, description = "Token de invitación", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Token válido"),
                                    @ApiResponse(responseCode = "404", description = "Token no encontrado"),
                                    @ApiResponse(responseCode = "410", description = "Token expirado"),
                                    @ApiResponse(responseCode = "409", description = "Token ya consumido")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/invitations/owner/register",
                    method = RequestMethod.POST,
                    beanClass = OwnerInvitationHandler.class,
                    beanMethod = "acceptOwnerRegistration",
                    operation = @Operation(
                            operationId = "registerOwner",
                            summary = "Registro de propietario",
                            description = "Completa el auto-registro de un propietario mediante token de invitación. Endpoint externo sin autenticación.",
                            tags = {"Owner Invitations"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = OwnerRegistrationRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Registro completado"),
                                    @ApiResponse(responseCode = "400", description = "Error de validación"),
                                    @ApiResponse(responseCode = "404", description = "Token no encontrado"),
                                    @ApiResponse(responseCode = "410", description = "Token expirado"),
                                    @ApiResponse(responseCode = "409", description = "Token ya consumido")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> ownerInvitationRoutes(OwnerInvitationHandler handler) {
        return route(POST("/api/invitations/owner").and(accept(MediaType.APPLICATION_JSON)), handler::createOwnerInvitation)
                .andRoute(GET("/api/external/invitations/validate"), handler::validateToken)
                .andRoute(POST("/api/external/invitations/owner/register").and(accept(MediaType.APPLICATION_JSON)), handler::acceptOwnerRegistration);
    }
}
