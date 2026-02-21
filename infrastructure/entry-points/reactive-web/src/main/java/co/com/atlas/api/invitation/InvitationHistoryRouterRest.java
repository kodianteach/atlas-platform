package co.com.atlas.api.invitation;

import co.com.atlas.api.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
 * Router for invitation history and user/unit lookup endpoints.
 */
@Configuration
@Tag(name = "Invitation History & Lookup", description = "Historial de invitaciones y búsqueda de usuarios/unidades")
public class InvitationHistoryRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/invitations/history",
                    method = RequestMethod.GET,
                    beanClass = InvitationHistoryHandler.class,
                    beanMethod = "getInvitationHistory",
                    operation = @Operation(
                            operationId = "getInvitationHistory",
                            summary = "Obtener historial de invitaciones",
                            description = "Obtiene el historial de invitaciones con filtros opcionales. Datos scopeados por rol.",
                            tags = {"Invitation History & Lookup"},
                            parameters = {
                                    @Parameter(name = "type", in = ParameterIn.QUERY, description = "Tipo de invitación (OWNER_SELF_REGISTER, RESIDENT_INVITE, etc.)"),
                                    @Parameter(name = "status", in = ParameterIn.QUERY, description = "Estado (PENDING, ACCEPTED, EXPIRED, CANCELLED)"),
                                    @Parameter(name = "unitId", in = ParameterIn.QUERY, description = "ID de unidad"),
                                    @Parameter(name = "search", in = ParameterIn.QUERY, description = "Búsqueda por email"),
                                    @Parameter(name = "dateFrom", in = ParameterIn.QUERY, description = "Fecha desde (ISO 8601)"),
                                    @Parameter(name = "dateTo", in = ParameterIn.QUERY, description = "Fecha hasta (ISO 8601)")
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Historial obtenido")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/users/lookup",
                    method = RequestMethod.GET,
                    beanClass = UserLookupHandler.class,
                    beanMethod = "lookupByDocument",
                    operation = @Operation(
                            operationId = "lookupUserByDocument",
                            summary = "Buscar usuario por documento",
                            description = "Busca un usuario existente por tipo y número de documento para autocompletado en formularios.",
                            tags = {"Invitation History & Lookup"},
                            parameters = {
                                    @Parameter(name = "documentType", in = ParameterIn.QUERY, description = "Tipo de documento", required = true),
                                    @Parameter(name = "documentNumber", in = ParameterIn.QUERY, description = "Número de documento", required = true)
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Resultado de búsqueda"),
                                    @ApiResponse(responseCode = "400", description = "Parámetros requeridos faltantes")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/users/lookup-email",
                    method = RequestMethod.GET,
                    beanClass = UserLookupHandler.class,
                    beanMethod = "lookupByEmail",
                    operation = @Operation(
                            operationId = "lookupUserByEmail",
                            summary = "Buscar usuario por email",
                            description = "Busca un usuario existente por email para autocompletado en formularios.",
                            tags = {"Invitation History & Lookup"},
                            parameters = {
                                    @Parameter(name = "email", in = ParameterIn.QUERY, description = "Email del usuario", required = true)
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Resultado de búsqueda"),
                                    @ApiResponse(responseCode = "400", description = "Email requerido")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/search",
                    method = RequestMethod.GET,
                    beanClass = UserLookupHandler.class,
                    beanMethod = "searchUnits",
                    operation = @Operation(
                            operationId = "searchUnits",
                            summary = "Buscar unidades por código",
                            description = "Busca unidades por prefijo del código para autocompletado. Máximo 20 resultados.",
                            tags = {"Invitation History & Lookup"},
                            parameters = {
                                    @Parameter(name = "query", in = ParameterIn.QUERY, description = "Prefijo del código de unidad", required = true)
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de unidades encontradas"),
                                    @ApiResponse(responseCode = "400", description = "X-Organization-Id header requerido")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> invitationHistoryRoutes(
            InvitationHistoryHandler historyHandler,
            UserLookupHandler lookupHandler) {
        return route(GET("/api/invitations/history"), historyHandler::getInvitationHistory)
                .andRoute(GET("/api/external/users/lookup"), lookupHandler::lookupByDocument)
                .andRoute(GET("/api/external/users/lookup-email"), lookupHandler::lookupByEmail)
                .andRoute(GET("/api/units/search"), lookupHandler::searchUnits);
    }
}
