package co.com.atlas.api.external;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.external.dto.ActivateAdminRequest;
import co.com.atlas.api.external.dto.ActivateAdminResponse;
import co.com.atlas.api.external.dto.CompleteOnboardingRequest;
import co.com.atlas.api.external.dto.CompleteOnboardingResponse;
import co.com.atlas.api.external.dto.PreRegisterAdminRequest;
import co.com.atlas.api.external.dto.PreRegisterAdminResponse;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.usecase.preregistration.ActivateAdminUseCase;
import co.com.atlas.usecase.preregistration.CompleteOnboardingUseCase;
import co.com.atlas.usecase.preregistration.PreRegisterAdminUseCase;
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
 * Handler para operaciones de pre-registro externo de administradores.
 * Esta API es EXTERNA e INDEPENDIENTE del flujo normal de auto-registro.
 * 
 * Flujo:
 * 1. Operador crea pre-registro → genera token y envía email
 * 2. Usuario valida token
 * 3. Usuario activa cuenta con credenciales temporales
 * 4. Usuario completa onboarding (crea Company y Organization)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalAdminHandler {

    private final PreRegisterAdminUseCase preRegisterAdminUseCase;
    private final ActivateAdminUseCase activateAdminUseCase;
    private final CompleteOnboardingUseCase completeOnboardingUseCase;

    /**
     * Pre-registra un nuevo administrador.
     * Solo operadores autorizados pueden invocar este endpoint.
     * 
     * POST /api/external/admin/pre-register
     */
    public Mono<ServerResponse> preRegister(ServerRequest request) {
        String operatorId = request.headers().firstHeader("X-Operator-Id");
        String operatorIp = request.headers().firstHeader("X-Forwarded-For");
        if (operatorIp == null) {
            operatorIp = request.remoteAddress()
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown");
        }
        
        final String finalOperatorIp = operatorIp;
        
        String operatorUserAgent = request.headers().firstHeader("User-Agent");
        
        return request.bodyToMono(PreRegisterAdminRequest.class)
                .flatMap(req -> {
                    Long parsedOperatorId = null;
                    if (operatorId != null) {
                        try {
                            parsedOperatorId = Long.parseLong(operatorId);
                        } catch (NumberFormatException e) {
                            // Use null if not parseable
                        }
                    }
                    
                    PreRegisterAdminUseCase.PreRegisterCommand command = 
                            new PreRegisterAdminUseCase.PreRegisterCommand(
                                    req.getEmail(),
                                    req.getNames(),
                                    req.getPhone(),
                                    req.getExpirationHours() != null ? req.getExpirationHours() : 72,
                                    req.getActivationBaseUrl(),
                                    parsedOperatorId,
                                    finalOperatorIp,
                                    operatorUserAgent,
                                    req.getMetadata()
                            );
                    
                    return preRegisterAdminUseCase.execute(command);
                })
                .flatMap(result -> {
                    PreRegisterAdminResponse data = PreRegisterAdminResponse.builder()
                            .userId(result.userId())
                            .tokenId(result.tokenId())
                            .email(result.email())
                            .names(result.names())
                            .expiresAt(result.expiresAt())
                            .message(result.message())
                            .build();
                    
                    ApiResponse<PreRegisterAdminResponse> response = ApiResponse.<PreRegisterAdminResponse>builder()
                            .success(true)
                            .status(HttpStatus.CREATED.value())
                            .message("Administrador pre-registrado exitosamente")
                            .data(data)
                            .build();
                    
                    return ServerResponse.status(HttpStatus.CREATED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(DuplicateException.class, e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "PREREGISTER_001");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.CONFLICT.value(),
                            e.getMessage(),
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.CONFLICT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "PREREGISTER_002");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage(),
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    /**
     * Valida un token de activación sin consumirlo.
     * 
     * GET /api/external/admin/validate-token?token={token}
     */
    public Mono<ServerResponse> validateToken(ServerRequest request) {
        String token = request.queryParam("token").orElse(null);
        
        if (token == null || token.isBlank()) {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Token es requerido",
                    request.path(),
                    null
            );
            
            return ServerResponse.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }
        
        return activateAdminUseCase.validateToken(token)
                .flatMap(result -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("valid", result.valid());
                    data.put("email", result.email());
                    data.put("names", result.names());
                    data.put("expiresAt", result.expiresAt());
                    
                    ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .status(HttpStatus.OK.value())
                            .message(result.valid() ? "Token válido" : "Token inválido")
                            .data(data)
                            .build();
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "VALIDATE_001");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Error validando token",
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    /**
     * Activa la cuenta de un administrador pre-registrado.
     * 
     * POST /api/external/admin/activate
     */
    public Mono<ServerResponse> activate(ServerRequest request) {
        String clientIp = request.headers().firstHeader("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.remoteAddress()
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown");
        }
        String userAgent = request.headers().firstHeader("User-Agent");
        
        final String finalClientIp = clientIp;
        
        return request.bodyToMono(ActivateAdminRequest.class)
                .flatMap(req -> {
                    ActivateAdminUseCase.ActivateCommand command =
                            new ActivateAdminUseCase.ActivateCommand(
                                    req.getToken(),
                                    req.getEmail(),
                                    req.getCurrentPassword(),
                                    req.getNewPassword(),
                                    finalClientIp,
                                    userAgent
                            );
                    
                    return activateAdminUseCase.execute(command);
                })
                .flatMap(result -> {
                    ActivateAdminResponse data = ActivateAdminResponse.builder()
                            .userId(result.userId())
                            .email(result.email())
                            .names(result.names())
                            .status(result.status() != null ? result.status().name() : null)
                            .message(result.message())
                            .build();
                    
                    ApiResponse<ActivateAdminResponse> response = ApiResponse.<ActivateAdminResponse>builder()
                            .success(true)
                            .status(HttpStatus.OK.value())
                            .message("Cuenta activada exitosamente")
                            .data(data)
                            .build();
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(IllegalStateException.class, e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "ACTIVATE_001");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage(),
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "ACTIVATE_002");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.UNAUTHORIZED.value(),
                            e.getMessage(),
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    /**
     * Completa el onboarding creando Company y Organization.
     * Solo usuarios con estado ACTIVATED pueden invocar este endpoint.
     * 
     * POST /api/external/admin/complete-onboarding
     */
    public Mono<ServerResponse> completeOnboarding(ServerRequest request) {
        return request.bodyToMono(CompleteOnboardingRequest.class)
                .flatMap(req -> {
                    CompleteOnboardingUseCase.OnboardingCommand command =
                            new CompleteOnboardingUseCase.OnboardingCommand(
                                    req.getUserId(),
                                    req.getCompanyName(),
                                    req.getCompanyTaxId(),
                                    req.getCompanyIndustry(),
                                    req.getCompanyAddress(),
                                    req.getCompanyCountry(),
                                    req.getCompanyCity(),
                                    req.getOrganizationName(),
                                    req.getOrganizationCode(),
                                    req.getOrganizationType(),
                                    req.getUsesZones(),
                                    req.getOrganizationDescription()
                            );
                    
                    return completeOnboardingUseCase.execute(command);
                })
                .flatMap(result -> {
                    CompleteOnboardingResponse data = CompleteOnboardingResponse.builder()
                            .companyId(result.companyId())
                            .companySlug(result.companySlug())
                            .organizationId(result.organizationId())
                            .organizationCode(result.organizationCode())
                            .message(result.message())
                            .build();
                    
                    ApiResponse<CompleteOnboardingResponse> response = ApiResponse.<CompleteOnboardingResponse>builder()
                            .success(true)
                            .status(HttpStatus.CREATED.value())
                            .message("Onboarding completado exitosamente")
                            .data(data)
                            .build();
                    
                    return ServerResponse.status(HttpStatus.CREATED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(IllegalStateException.class, e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "ONBOARDING_001");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.FORBIDDEN.value(),
                            e.getMessage(),
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("errorCode", "ONBOARDING_002");
                    
                    ApiResponse<Void> response = ApiResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage(),
                            request.path(),
                            metadata
                    );
                    
                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }
}
