package co.com.atlas.api.access;

import co.com.atlas.api.access.dto.ValidateCodeRequest;
import co.com.atlas.api.access.dto.ValidateCodeResponse;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.usecase.access.AccessCodeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para operaciones de AccessCode.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessHandler {

    private final AccessCodeUseCase accessCodeUseCase;

    public Mono<ServerResponse> validateCode(ServerRequest request) {
        return request.bodyToMono(ValidateCodeRequest.class)
                .flatMap(req -> {
                    Long scannedBy = extractUserIdFromRequest(request);
                    return accessCodeUseCase.validateCode(
                            req.getCode(), 
                            scannedBy, 
                            req.getScanLocation(), 
                            req.getDeviceInfo()
                    );
                })
                .flatMap(scanLog -> {
                    boolean isValid = scanLog.getScanResult() == ScanResult.VALID;
                    
                    ValidateCodeResponse data = ValidateCodeResponse.builder()
                            .valid(isValid)
                            .scanResult(scanLog.getScanResult().name())
                            .message(getMessageForResult(scanLog.getScanResult()))
                            .build();
                    
                    ApiResponse<ValidateCodeResponse> response = ApiResponse.success(data, 
                            isValid ? "Código válido" : "Código inválido");
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getByVisitRequest(ServerRequest request) {
        Long visitRequestId = Long.parseLong(request.pathVariable("visitRequestId"));
        return accessCodeUseCase.findAllByVisitRequestId(visitRequestId)
                .collectList()
                .flatMap(codes -> {
                    ApiResponse<Object> response = ApiResponse.success(codes, "Códigos obtenidos");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> getScanLogs(ServerRequest request) {
        Long accessCodeId = Long.parseLong(request.pathVariable("accessCodeId"));
        return accessCodeUseCase.getScanLogs(accessCodeId)
                .collectList()
                .flatMap(logs -> {
                    ApiResponse<Object> response = ApiResponse.success(logs, "Logs de escaneo obtenidos");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> revokeCode(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return accessCodeUseCase.revoke(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Código revocado");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    private Long extractUserIdFromRequest(ServerRequest request) {
        return request.headers().firstHeader("X-User-Id") != null 
                ? Long.parseLong(request.headers().firstHeader("X-User-Id")) 
                : 1L;
    }

    private String getMessageForResult(ScanResult result) {
        return switch (result) {
            case VALID -> "Acceso permitido";
            case INVALID -> "Código no reconocido";
            case EXPIRED -> "Código expirado";
            case ALREADY_USED -> "Código ya utilizado el máximo de veces permitidas";
            case REVOKED -> "Código revocado";
        };
    }

    private Mono<ServerResponse> buildErrorResponse(Exception e, HttpStatus status, String path) {
        ApiResponse<Void> response = ApiResponse.error(status.value(), e.getMessage(), path, null);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
