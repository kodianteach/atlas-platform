package co.com.atlas.api.external;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.external.dto.EnrollDeviceRequest;
import co.com.atlas.api.external.dto.EnrollDeviceResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.porter.DeviceInfo;
import co.com.atlas.usecase.porter.EnrollPorterDeviceUseCase;
import co.com.atlas.usecase.porter.ValidateEnrollmentTokenUseCase;
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
 * Handler para endpoints públicos de enrolamiento de dispositivos de portería.
 * API PÚBLICA sin autenticación — accedida por dispositivos que aún no tienen sesión.
 *
 * Endpoints:
 * 1. GET  /api/external/porter/validate-token?token={token} — Validar token sin consumir
 * 2. POST /api/external/porter/enroll — Enrolar dispositivo y obtener clave pública
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalPorterHandler {

    private final ValidateEnrollmentTokenUseCase validateEnrollmentTokenUseCase;
    private final EnrollPorterDeviceUseCase enrollPorterDeviceUseCase;

    /**
     * Valida un token de enrolamiento sin consumirlo.
     * Permite al frontend verificar el token antes de mostrar la pantalla de enrolamiento.
     *
     * GET /api/external/porter/validate-token?token={token}
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

        return validateEnrollmentTokenUseCase.execute(token)
                .flatMap(result -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("valid", result.valid());
                    data.put("porterName", result.porterName());
                    data.put("organizationName", result.organizationName());
                    data.put("expiresAt", result.expiresAt());

                    ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .status(HttpStatus.OK.value())
                            .message("Token válido")
                            .data(data)
                            .build();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(e -> handleError(e, request));
    }

    /**
     * Enrola un dispositivo de portería.
     * Consume el token, genera/obtiene claves, activa el portero
     * y retorna la clave pública JWK para verificación offline.
     *
     * POST /api/external/porter/enroll
     */
    public Mono<ServerResponse> enroll(ServerRequest request) {
        String clientIp = request.headers().firstHeader("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.remoteAddress()
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown");
        }
        String userAgent = request.headers().firstHeader("User-Agent");

        final String finalClientIp = clientIp;

        return request.bodyToMono(EnrollDeviceRequest.class)
                .flatMap(req -> {
                    DeviceInfo deviceInfo = new DeviceInfo(
                            req.getPlatform(),
                            req.getModel(),
                            req.getAppVersion(),
                            userAgent
                    );

                    EnrollPorterDeviceUseCase.EnrollCommand command =
                            new EnrollPorterDeviceUseCase.EnrollCommand(
                                    req.getToken(),
                                    deviceInfo,
                                    finalClientIp,
                                    userAgent
                            );

                    return enrollPorterDeviceUseCase.execute(command);
                })
                .flatMap(result -> {
                    EnrollDeviceResponse data = EnrollDeviceResponse.builder()
                            .porterId(result.porterId())
                            .porterDisplayName(result.porterDisplayName())
                            .organizationName(result.organizationName())
                            .verificationKeyJwk(result.verificationKeyJwk())
                            .keyId(result.keyId())
                            .maxClockSkewMinutes(result.maxClockSkewMinutes())
                            .build();

                    ApiResponse<EnrollDeviceResponse> response = ApiResponse.<EnrollDeviceResponse>builder()
                            .success(true)
                            .status(HttpStatus.OK.value())
                            .message("Dispositivo enrolado exitosamente")
                            .data(data)
                            .build();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(e -> handleError(e, request));
    }

    /**
     * Método centralizado para manejar errores.
     */
    private Mono<ServerResponse> handleError(Throwable e, ServerRequest request) {
        log.error("Error en porter endpoint [{}]: {} - {}", request.path(),
                e.getClass().getSimpleName(), e.getMessage());

        HttpStatus status;
        String errorCode;
        String message = e.getMessage();

        if (e instanceof BusinessException be) {
            status = HttpStatus.resolve(be.getHttpStatus());
            if (status == null) status = HttpStatus.BAD_REQUEST;
            errorCode = be.getErrorCode();
            message = be.getMessage();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_ERROR";
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("errorCode", errorCode);

        ApiResponse<Void> response = ApiResponse.error(
                status.value(),
                message,
                request.path(),
                metadata
        );

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
