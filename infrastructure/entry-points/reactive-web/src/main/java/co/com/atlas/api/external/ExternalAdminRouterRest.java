package co.com.atlas.api.external;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.external.dto.ActivateAdminRequest;
import co.com.atlas.api.external.dto.ActivateAdminResponse;
import co.com.atlas.api.external.dto.CompleteOnboardingRequest;
import co.com.atlas.api.external.dto.CompleteOnboardingResponse;
import co.com.atlas.api.external.dto.PreRegisterAdminRequest;
import co.com.atlas.api.external.dto.PreRegisterAdminResponse;
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
 * Router para endpoints de pre-registro externo de administradores.
 * 
 * Esta API es EXTERNA e INDEPENDIENTE del flujo normal de auto-registro.
 * Permite a operadores de plataforma crear usuarios administradores pre-registrados
 * que luego pueden activar su cuenta y crear su Company/Organization.
 * 
 * Flujo:
 * 1. POST /pre-register → Operador crea pre-registro
 * 2. GET /validate-token → Usuario valida token
 * 3. POST /activate → Usuario activa cuenta
 * 4. POST /complete-onboarding → Usuario crea Company/Organization
 */
@Configuration
@Tag(name = "Pre-Registro Externo", description = "API externa para pre-registro y activación de administradores de conjuntos residenciales")
public class ExternalAdminRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/external/admin/pre-register",
                    method = RequestMethod.POST,
                    beanClass = ExternalAdminHandler.class,
                    beanMethod = "preRegister",
                    operation = @Operation(
                            operationId = "preRegisterAdmin",
                            summary = "Pre-registrar administrador",
                            description = """
                                    Crea un usuario pre-registrado para un futuro administrador de conjunto.
                                    
                                    **Solo operadores autorizados pueden invocar este endpoint.**
                                    
                                    El sistema:
                                    - Crea el usuario con estado PRE_REGISTERED
                                    - Genera credenciales temporales seguras
                                    - Genera un token de activación con expiración configurable
                                    - Envía email con credenciales y link de activación
                                    
                                    Headers requeridos:
                                    - X-Operator-Id: ID del operador que realiza la acción
                                    """,
                            tags = {"Pre-Registro Externo"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = PreRegisterAdminRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Administrador pre-registrado exitosamente",
                                            content = @Content(schema = @Schema(implementation = PreRegisterAdminResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Datos de entrada inválidos",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "409",
                                            description = "Email ya registrado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/admin/validate-token",
                    method = RequestMethod.GET,
                    beanClass = ExternalAdminHandler.class,
                    beanMethod = "validateToken",
                    operation = @Operation(
                            operationId = "validateActivationToken",
                            summary = "Validar token de activación",
                            description = """
                                    Valida un token de activación sin consumirlo.
                                    
                                    Permite al frontend verificar si el token es válido antes
                                    de mostrar el formulario de activación.
                                    
                                    Retorna:
                                    - valid: true/false
                                    - email: email del usuario (si válido)
                                    - names: nombre del usuario (si válido)
                                    - expiresAt: fecha de expiración (si válido)
                                    """,
                            tags = {"Pre-Registro Externo"},
                            parameters = {
                                    @Parameter(
                                            name = "token",
                                            description = "Token de activación recibido por email",
                                            required = true,
                                            in = ParameterIn.QUERY
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Resultado de validación del token"
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Token no proporcionado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/admin/activate",
                    method = RequestMethod.POST,
                    beanClass = ExternalAdminHandler.class,
                    beanMethod = "activate",
                    operation = @Operation(
                            operationId = "activateAdmin",
                            summary = "Activar cuenta de administrador",
                            description = """
                                    Activa la cuenta de un administrador pre-registrado.
                                    
                                    Requiere:
                                    - Token de activación válido (no expirado, no consumido)
                                    - Email del usuario
                                    - Contraseña temporal (enviada por email)
                                    - Nueva contraseña (elegida por el usuario)
                                    
                                    El sistema:
                                    - Valida el token y las credenciales temporales
                                    - Actualiza la contraseña del usuario
                                    - Cambia el estado del usuario a ACTIVATED
                                    - Marca el token como consumido (single-use)
                                    - Registra la acción en el log de auditoría
                                    
                                    Después de la activación, el usuario puede proceder
                                    a crear su Company y Organization.
                                    """,
                            tags = {"Pre-Registro Externo"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = ActivateAdminRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Cuenta activada exitosamente",
                                            content = @Content(schema = @Schema(implementation = ActivateAdminResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Token inválido, expirado o ya consumido",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "401",
                                            description = "Credenciales temporales incorrectas",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/external/admin/complete-onboarding",
                    method = RequestMethod.POST,
                    beanClass = ExternalAdminHandler.class,
                    beanMethod = "completeOnboarding",
                    operation = @Operation(
                            operationId = "completeOnboarding",
                            summary = "Completar onboarding",
                            description = """
                                    Crea la Company y Organization para un administrador activado.
                                    
                                    **IMPORTANTE**: Solo usuarios con estado ACTIVATED pueden invocar este endpoint.
                                    Sin un token de activación válido previo, NO es posible crear Company/Organization.
                                    
                                    El sistema:
                                    - Valida que el usuario tenga estado ACTIVATED
                                    - Crea la Company con los datos proporcionados
                                    - Crea la Organization (conjunto residencial)
                                    - Asocia el usuario como administrador
                                    - Cambia el estado del usuario a ACTIVE
                                    
                                    Después de esto, el usuario puede iniciar sesión normalmente
                                    y gestionar su conjunto residencial.
                                    """,
                            tags = {"Pre-Registro Externo"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = CompleteOnboardingRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Onboarding completado exitosamente",
                                            content = @Content(schema = @Schema(implementation = CompleteOnboardingResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Datos de entrada inválidos",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "403",
                                            description = "Usuario no tiene estado ACTIVATED",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> externalAdminRoutes(ExternalAdminHandler handler) {
        return route(POST("/api/external/admin/pre-register").and(accept(MediaType.APPLICATION_JSON)), handler::preRegister)
                .andRoute(GET("/api/external/admin/validate-token"), handler::validateToken)
                .andRoute(POST("/api/external/admin/activate").and(accept(MediaType.APPLICATION_JSON)), handler::activate)
                .andRoute(POST("/api/external/admin/complete-onboarding").and(accept(MediaType.APPLICATION_JSON)), handler::completeOnboarding);
    }
}
