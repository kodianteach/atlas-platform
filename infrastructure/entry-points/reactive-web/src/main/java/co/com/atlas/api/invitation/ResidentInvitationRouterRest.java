package co.com.atlas.api.invitation;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.invitation.dto.CreateResidentInvitationRequest;
import co.com.atlas.api.invitation.dto.ResidentRegistrationRequest;
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
 * Router for resident invitation endpoints.
 * Includes authenticated (owner) and external (self-register) routes.
 */
@Configuration
@Tag(name = "Resident Invitations", description = "Gestión de invitaciones de residentes por propietario")
public class ResidentInvitationRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/invitations/resident",
                    method = RequestMethod.POST,
                    beanClass = ResidentInvitationHandler.class,
                    beanMethod = "createResidentInvitation",
                    operation = @Operation(
                            operationId = "createResidentInvitation",
                            summary = "Generar invitación de residente",
                            description = "Genera un enlace de invitación para que un residente se registre en la unidad del propietario. Solo OWNER.",
                            tags = {"Resident Invitations"},
                            requestBody = @RequestBody(
                                    content = @Content(schema = @Schema(implementation = CreateResidentInvitationRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Invitación creada"),
                                    @ApiResponse(responseCode = "404", description = "Unidad o organización no encontrada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/invitations/resident/register",
                    method = RequestMethod.POST,
                    beanClass = ResidentInvitationHandler.class,
                    beanMethod = "acceptResidentRegistration",
                    operation = @Operation(
                            operationId = "registerResident",
                            summary = "Registro de residente",
                            description = "Completa el registro de un residente mediante token de invitación. Endpoint externo sin autenticación.",
                            tags = {"Resident Invitations"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = ResidentRegistrationRequest.class))
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
    public RouterFunction<ServerResponse> residentInvitationRoutes(ResidentInvitationHandler handler) {
        return route(POST("/api/invitations/resident").and(accept(MediaType.APPLICATION_JSON)), handler::createResidentInvitation)
                .andRoute(POST("/api/external/invitations/resident/register").and(accept(MediaType.APPLICATION_JSON)), handler::acceptResidentRegistration);
    }
}
