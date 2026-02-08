package co.com.atlas.api.invitation;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.invitation.dto.AcceptInvitationRequest;
import co.com.atlas.api.invitation.dto.InvitationRequest;
import co.com.atlas.api.invitation.dto.InvitationResponse;
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
 * Router para endpoints de Invitation.
 */
@Configuration
@Tag(name = "Invitations", description = "Gestión de invitaciones")
public class InvitationRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/invitations",
                    method = RequestMethod.POST,
                    beanClass = InvitationHandler.class,
                    beanMethod = "create",
                    operation = @Operation(
                            operationId = "createInvitation",
                            summary = "Crear invitación",
                            description = "Crea una invitación para unirse a organización/unidad",
                            tags = {"Invitations"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = InvitationRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Invitación creada",
                                            content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/invitations/{id}",
                    method = RequestMethod.GET,
                    beanClass = InvitationHandler.class,
                    beanMethod = "getById",
                    operation = @Operation(
                            operationId = "getInvitationById",
                            summary = "Obtener invitación por ID",
                            tags = {"Invitations"},
                            parameters = @Parameter(name = "id", description = "ID de la invitación", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Invitación encontrada"),
                                    @ApiResponse(responseCode = "404", description = "Invitación no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/invitations/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = InvitationHandler.class,
                    beanMethod = "getByOrganization",
                    operation = @Operation(
                            operationId = "getInvitationsByOrganization",
                            summary = "Listar invitaciones por organización",
                            tags = {"Invitations"},
                            parameters = @Parameter(name = "organizationId", description = "ID de la organización", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de invitaciones")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/invitations/unit/{unitId}",
                    method = RequestMethod.GET,
                    beanClass = InvitationHandler.class,
                    beanMethod = "getByUnit",
                    operation = @Operation(
                            operationId = "getInvitationsByUnit",
                            summary = "Listar invitaciones por unidad",
                            tags = {"Invitations"},
                            parameters = @Parameter(name = "unitId", description = "ID de la unidad", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de invitaciones")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/invitations/accept",
                    method = RequestMethod.POST,
                    beanClass = InvitationHandler.class,
                    beanMethod = "accept",
                    operation = @Operation(
                            operationId = "acceptInvitation",
                            summary = "Aceptar invitación",
                            description = "Acepta una invitación mediante token",
                            tags = {"Invitations"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = AcceptInvitationRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Invitación aceptada"),
                                    @ApiResponse(responseCode = "400", description = "Token inválido o expirado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/invitations/{id}/cancel",
                    method = RequestMethod.POST,
                    beanClass = InvitationHandler.class,
                    beanMethod = "cancel",
                    operation = @Operation(
                            operationId = "cancelInvitation",
                            summary = "Cancelar invitación",
                            tags = {"Invitations"},
                            parameters = @Parameter(name = "id", description = "ID de la invitación", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Invitación cancelada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/invitations/{id}/resend",
                    method = RequestMethod.POST,
                    beanClass = InvitationHandler.class,
                    beanMethod = "resend",
                    operation = @Operation(
                            operationId = "resendInvitation",
                            summary = "Reenviar invitación",
                            tags = {"Invitations"},
                            parameters = @Parameter(name = "id", description = "ID de la invitación", required = true),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Invitación reenviada")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> invitationRoutes(InvitationHandler handler) {
        return route(POST("/api/invitations").and(accept(MediaType.APPLICATION_JSON)), handler::create)
                .andRoute(GET("/api/invitations/{id}"), handler::getById)
                .andRoute(GET("/api/invitations/organization/{organizationId}"), handler::getByOrganization)
                .andRoute(GET("/api/invitations/unit/{unitId}"), handler::getByUnit)
                .andRoute(POST("/api/invitations/accept").and(accept(MediaType.APPLICATION_JSON)), handler::accept)
                .andRoute(POST("/api/invitations/{id}/cancel"), handler::cancel)
                .andRoute(POST("/api/invitations/{id}/resend"), handler::resend);
    }
}
