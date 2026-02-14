package co.com.atlas.api.activation;

import co.com.atlas.api.activation.dto.CompleteOwnerActivationRequest;
import co.com.atlas.api.activation.dto.CompleteOwnerActivationResponse;
import co.com.atlas.api.activation.dto.ValidateOwnerTokenResponse;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.usecase.activation.OwnerActivationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para activación de propietarios invitados.
 * 
 * Este endpoint es PÚBLICO (no requiere autenticación).
 * El propietario recibe un token por correo y usa estos endpoints
 * para validar y completar su activación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerActivationHandler {

    private final OwnerActivationUseCase ownerActivationUseCase;

    /**
     * Valida un token de invitación de propietario.
     * 
     * GET /api/activation/owner/validate/{token}
     */
    public Mono<ServerResponse> validateToken(ServerRequest request) {
        String token = request.pathVariable("token");
        
        if (token == null || token.isBlank()) {
            return buildErrorResponse("Token requerido", HttpStatus.BAD_REQUEST);
        }
        
        return ownerActivationUseCase.validateToken(token)
                .flatMap(result -> {
                    ValidateOwnerTokenResponse response = ValidateOwnerTokenResponse.builder()
                            .valid(result.isValid())
                            .email(result.getEmail())
                            .names(result.getNames())
                            .organizationName(result.getOrganizationName())
                            .unitCode(result.getUnitCode())
                            .invitationId(result.getInvitationId())
                            .userExists(result.isUserExists())
                            .message(result.getMessage())
                            .errorCode(result.getErrorCode())
                            .build();
                    
                    ApiResponse<ValidateOwnerTokenResponse> apiResponse = 
                            ApiResponse.success(response, result.isValid() ? "Token válido" : result.getMessage());
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                })
                .onErrorResume(NotFoundException.class, e -> 
                        buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND))
                .onErrorResume(BusinessException.class, e -> 
                        buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
    }

    /**
     * Completa la activación del propietario estableciendo su contraseña.
     * 
     * POST /api/activation/owner/complete
     */
    public Mono<ServerResponse> completeActivation(ServerRequest request) {
        return request.bodyToMono(CompleteOwnerActivationRequest.class)
                .flatMap(req -> {
                    if (req.getToken() == null || req.getToken().isBlank()) {
                        return Mono.error(new BusinessException("Token requerido"));
                    }
                    if (req.getPassword() == null || req.getPassword().isBlank()) {
                        return Mono.error(new BusinessException("Contraseña requerida"));
                    }
                    if (req.getConfirmPassword() == null || req.getConfirmPassword().isBlank()) {
                        return Mono.error(new BusinessException("Confirmación de contraseña requerida"));
                    }
                    
                    return ownerActivationUseCase.completeActivation(
                            req.getToken(),
                            req.getPassword(),
                            req.getConfirmPassword()
                    );
                })
                .flatMap(user -> {
                    CompleteOwnerActivationResponse response = CompleteOwnerActivationResponse.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .names(user.getNames())
                            .organizationId(user.getOrganizationId())
                            .message("Cuenta activada exitosamente. Ya puede iniciar sesión.")
                            .success(true)
                            .build();
                    
                    ApiResponse<CompleteOwnerActivationResponse> apiResponse = 
                            ApiResponse.success(response, "Activación completada");
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                })
                .onErrorResume(NotFoundException.class, e -> 
                        buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND))
                .onErrorResume(BusinessException.class, e -> 
                        buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private Mono<ServerResponse> buildErrorResponse(String message, HttpStatus status) {
        ApiResponse<Object> errorResponse = ApiResponse.error(message);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }
}
