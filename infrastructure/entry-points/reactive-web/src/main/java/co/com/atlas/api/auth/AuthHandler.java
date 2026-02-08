package co.com.atlas.api.auth;

import co.com.atlas.api.auth.dto.LoginRequest;
import co.com.atlas.api.auth.dto.LoginResponse;
import co.com.atlas.api.auth.dto.RefreshTokenRequest;
import co.com.atlas.api.auth.dto.RegisterRequest;
import co.com.atlas.api.auth.dto.RegisterResponse;
import co.com.atlas.api.auth.dto.VerifyTokenRequest;
import co.com.atlas.api.auth.dto.VerifyTokenResponse;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.auth.AuthCredentials;
import co.com.atlas.model.auth.AuthToken;
import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.usecase.auth.AuthenticationException;
import co.com.atlas.usecase.auth.LoginUseCase;
import co.com.atlas.usecase.auth.RefreshTokenUseCase;
import co.com.atlas.usecase.auth.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler para operaciones de autenticación.
 * Responsabilidades: login, refresh token, verify token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHandler {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final RegisterUserUseCase registerUserUseCase;
    private final JwtTokenGateway jwtTokenGateway;

    /**
     * Maneja el registro de nuevos usuarios.
     */
    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequest.class)
                .flatMap(registerRequest -> {
                    AuthUser newUser = AuthUser.builder()
                            .names(registerRequest.getNames())
                            .email(registerRequest.getEmail())
                            .passwordHash(registerRequest.getPassword())
                            .phone(registerRequest.getPhone())
                            .build();
                    return registerUserUseCase.execute(newUser);
                })
                .flatMap(user -> {
                    RegisterResponse data = RegisterResponse.builder()
                            .id(user.getId())
                            .names(user.getNames())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .message("Usuario registrado exitosamente")
                            .build();
                    
                    ApiResponse<RegisterResponse> response = ApiResponse.<RegisterResponse>builder()
                            .success(true)
                            .status(201)
                            .message("Usuario registrado exitosamente")
                            .data(data)
                            .build();
                    
                    return ServerResponse.status(HttpStatus.CREATED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(DuplicateException.class, e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "REG_001");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.CONFLICT.value(),
                            e.getMessage(),
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.CONFLICT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    /**
     * Maneja el inicio de sesión de usuarios.
     */
    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .flatMap(loginRequest -> {
                    AuthCredentials credentials = AuthCredentials.builder()
                            .email(loginRequest.getEmail())
                            .password(loginRequest.getPassword())
                            .build();
                    return loginUseCase.execute(credentials);
                })
                .flatMap(this::buildAuthResponse)
                .onErrorResume(AuthenticationException.class, e -> buildErrorResponse(e, request.path()));
    }

    /**
     * Maneja la renovación de tokens de acceso.
     */
    public Mono<ServerResponse> refreshToken(ServerRequest request) {
        return request.bodyToMono(RefreshTokenRequest.class)
                .flatMap(refreshRequest -> refreshTokenUseCase.execute(refreshRequest.getRefreshToken()))
                .flatMap(this::buildAuthResponse)
                .onErrorResume(AuthenticationException.class, e -> buildErrorResponse(e, request.path()));
    }

    /**
     * Verifica y decodifica un token JWT.
     */
    public Mono<ServerResponse> verifyToken(ServerRequest request) {
        return request.bodyToMono(VerifyTokenRequest.class)
                .flatMap(verifyRequest -> {
                    String token = verifyRequest.getToken();
                    
                    return jwtTokenGateway.validateToken(token)
                            .flatMap(isValid -> {
                                if (!isValid) {
                                    Map<String, Object> metadata = new HashMap<>();
                                    metadata.put("errorCode", "TOKEN_001");
                                    
                                    ApiResponse<Void> error = ApiResponse.error(
                                            HttpStatus.BAD_REQUEST.value(),
                                            "Token inválido o expirado",
                                            request.path(),
                                            metadata
                                    );
                                    
                                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .bodyValue(error);
                                }
                                
                                return Mono.zip(
                                        jwtTokenGateway.extractUserId(token),
                                        jwtTokenGateway.extractRole(token)
                                ).flatMap(tuple -> {
                                    String userId = tuple.getT1();
                                    String role = tuple.getT2();
                                    
                                    Map<String, Object> claimsMap = new HashMap<>();
                                    claimsMap.put("userId", userId);
                                    claimsMap.put("role", role);
                                    claimsMap.put("message", "Token válido - Decodifica el token en el frontend para ver toda la información");
                                    
                                    log.info("=== TOKEN VERIFICADO ===");
                                    log.info("User ID: {}", userId);
                                    log.info("Role: {}", role);
                                    log.info("=========================");
                                    
                                    VerifyTokenResponse data = VerifyTokenResponse.builder()
                                            .claims(claimsMap)
                                            .build();
                                    
                                    ApiResponse<VerifyTokenResponse> response = ApiResponse.<VerifyTokenResponse>builder()
                                            .success(true)
                                            .status(200)
                                            .message("Token válido. Ver consola y decodificar JWT para ver todos los claims.")
                                            .data(data)
                                            .build();
                                    
                                    return ServerResponse.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .bodyValue(response);
                                });
                            })
                            .onErrorResume(e -> {
                                log.error("Error al verificar token: {}", e.getMessage());
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("errorCode", "TOKEN_002");
                                
                                ApiResponse<Void> error = ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Error al verificar token: " + e.getMessage(),
                                        request.path(),
                                        metadata
                                );
                                
                                return ServerResponse.status(HttpStatus.BAD_REQUEST)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(error);
                            });
                });
    }

    private Mono<ServerResponse> buildAuthResponse(AuthToken authToken) {
        LoginResponse data = LoginResponse.builder()
                .accessToken(authToken.getAccessToken())
                .refreshToken(authToken.getRefreshToken())
                .tokenType(authToken.getTokenType())
                .build();

        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .success(true)
                .status(200)
                .message("Autenticación exitosa")
                .data(data)
                .build();

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

    private Mono<ServerResponse> buildErrorResponse(AuthenticationException e, String path) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("errorCode", "AUTH_001");
        
        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                e.getMessage(),
                path,
                metadata
        );

        return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
