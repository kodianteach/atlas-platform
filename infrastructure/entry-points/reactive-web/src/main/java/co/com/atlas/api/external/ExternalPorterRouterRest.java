package co.com.atlas.api.external;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.external.dto.EnrollDeviceRequest;
import co.com.atlas.api.external.dto.EnrollDeviceResponse;
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

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints públicos de enrolamiento de dispositivos de portería.
 *
 * API PÚBLICA sin autenticación.
 * Endpoints:
 * 1. GET  /api/external/porter/validate-token — Validar token sin consumir
 * 2. POST /api/external/porter/enroll — Enrolar dispositivo
 */
@Configuration
@Tag(name = "Enrolamiento de Portería", description = "API pública para enrolamiento de dispositivos de portería")
public class ExternalPorterRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/external/porter/validate-token",
                    method = RequestMethod.GET,
                    beanClass = ExternalPorterHandler.class,
                    beanMethod = "validateToken",
                    operation = @Operation(
                            operationId = "validateEnrollmentToken",
                            summary = "Validar token de enrolamiento",
                            description = """
                                    Valida un token de enrolamiento de portero sin consumirlo.

                                    Permite al frontend verificar si el token es válido antes
                                    de mostrar la pantalla de enrolamiento.

                                    Retorna:
                                    - valid: true/false
                                    - porterName: nombre del portero
                                    - organizationName: nombre de la organización
                                    - expiresAt: fecha de expiración
                                    """,
                            tags = {"Enrolamiento de Portería"},
                            parameters = {
                                    @Parameter(
                                            name = "token",
                                            description = "Token de enrolamiento recibido del enlace",
                                            required = true,
                                            in = ParameterIn.QUERY
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Token válido"
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Token no proporcionado o inválido",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "404",
                                            description = "Token no encontrado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/porter/enroll",
                    method = RequestMethod.POST,
                    beanClass = ExternalPorterHandler.class,
                    beanMethod = "enroll",
                    operation = @Operation(
                            operationId = "enrollPorterDevice",
                            summary = "Enrolar dispositivo de portería",
                            description = """
                                    Enrola un dispositivo de portería consumiendo el token de enrolamiento.

                                    El sistema:
                                    - Valida y consume el token (single-use)
                                    - Genera o reutiliza claves Ed25519 de la organización
                                    - Activa el usuario portero (PRE_REGISTERED → ACTIVE)
                                    - Registra auditoría con info del dispositivo
                                    - Retorna la clave pública JWK para verificación offline de QRs

                                    **IMPORTANTE**: Este endpoint consume el token. Solo puede usarse una vez.
                                    """,
                            tags = {"Enrolamiento de Portería"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = EnrollDeviceRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Dispositivo enrolado exitosamente",
                                            content = @Content(schema = @Schema(implementation = EnrollDeviceResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Token inválido, expirado o ya consumido",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "404",
                                            description = "Token no encontrado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> externalPorterRoutes(ExternalPorterHandler handler) {
        return route(GET("/api/external/porter/validate-token"), handler::validateToken)
                .andRoute(POST("/api/external/porter/enroll").and(accept(MediaType.APPLICATION_JSON)), handler::enroll);
    }
}
