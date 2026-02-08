package co.com.atlas.api.auth;

import co.com.atlas.api.auth.dto.LoginRequest;
import co.com.atlas.api.auth.dto.LoginResponse;
import co.com.atlas.api.auth.dto.RefreshTokenRequest;
import co.com.atlas.api.auth.dto.RegisterRequest;
import co.com.atlas.api.auth.dto.RegisterResponse;
import co.com.atlas.api.auth.dto.VerifyTokenRequest;
import co.com.atlas.api.auth.dto.VerifyTokenResponse;
import co.com.atlas.api.common.dto.ErrorResponse;
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

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints de autenticación.
 * Responsabilidades: login, refresh token, verify token.
 */
@Configuration
@Tag(name = "Autenticación", description = "Endpoints de autenticación y gestión de tokens JWT")
public class AuthRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/auth/register",
                    method = RequestMethod.POST,
                    beanClass = AuthHandler.class,
                    beanMethod = "register",
                    operation = @Operation(
                            operationId = "register",
                            summary = "Registrar nuevo usuario",
                            description = "Registra un nuevo usuario en el sistema",
                            tags = {"Autenticación"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = RegisterRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Usuario registrado exitosamente",
                                            content = @Content(schema = @Schema(implementation = RegisterResponse.class))
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
                    path = "/api/auth/login",
                    method = RequestMethod.POST,
                    beanClass = AuthHandler.class,
                    beanMethod = "login",
                    operation = @Operation(
                            operationId = "login",
                            summary = "Iniciar sesión",
                            description = "Autentica un usuario con email y contraseña y devuelve tokens JWT",
                            tags = {"Autenticación"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Login exitoso",
                                            content = @Content(schema = @Schema(implementation = LoginResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "401",
                                            description = "Credenciales inválidas",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/auth/refresh",
                    method = RequestMethod.POST,
                    beanClass = AuthHandler.class,
                    beanMethod = "refreshToken",
                    operation = @Operation(
                            operationId = "refreshToken",
                            summary = "Renovar token",
                            description = "Obtiene un nuevo access token usando un refresh token válido",
                            tags = {"Autenticación"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Token renovado exitosamente",
                                            content = @Content(schema = @Schema(implementation = LoginResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "401",
                                            description = "Refresh token inválido o expirado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/auth/verify-token",
                    method = RequestMethod.POST,
                    beanClass = AuthHandler.class,
                    beanMethod = "verifyToken",
                    operation = @Operation(
                            operationId = "verifyToken",
                            summary = "Verificar token JWT",
                            description = "Verifica y decodifica un token JWT mostrando su contenido",
                            tags = {"Autenticación"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = VerifyTokenRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Token verificado exitosamente",
                                            content = @Content(schema = @Schema(implementation = VerifyTokenResponse.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Token inválido o expirado",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> authRoutes(AuthHandler authHandler) {
        return route(POST("/api/auth/register").and(accept(MediaType.APPLICATION_JSON)), authHandler::register)
                .andRoute(POST("/api/auth/login").and(accept(MediaType.APPLICATION_JSON)), authHandler::login)
                .andRoute(POST("/api/auth/refresh").and(accept(MediaType.APPLICATION_JSON)), authHandler::refreshToken)
                .andRoute(POST("/api/auth/verify-token").and(accept(MediaType.APPLICATION_JSON)), authHandler::verifyToken);
    }
}
