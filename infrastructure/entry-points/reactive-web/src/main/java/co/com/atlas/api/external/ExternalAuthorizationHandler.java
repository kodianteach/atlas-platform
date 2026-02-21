package co.com.atlas.api.external;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.QrImageGeneratorGateway;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG;

/**
 * Handler para endpoints públicos de autorización de visitantes.
 * <p>
 * Estos endpoints NO requieren autenticación. Están diseñados para:
 * - Porteros que escanean el QR y verifican la autorización.
 * - Visitantes que consultan su propia información de acceso.
 * </p>
 * <p>
 * Ruta base: /api/external/authorizations
 * Configurada como permitAll en SecurityConfig.
 * </p>
 *
 * @author Atlas Platform Team
 * @since HU #6
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalAuthorizationHandler {

    private final VisitorAuthorizationRepository authorizationRepository;
    private final QrImageGeneratorGateway qrImageGeneratorGateway;

    /**
     * Obtiene los datos de verificación de una autorización.
     * Usado por porteros al escanear el QR para validar el ingreso.
     *
     * @param request ServerRequest con path variable "id"
     * @return ServerResponse con datos de la autorización y estado de vigencia
     */
    public Mono<ServerResponse> getQrData(ServerRequest request) {
        Long authorizationId = Long.parseLong(request.pathVariable("id"));

        return authorizationRepository.findById(authorizationId)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "Autorización no encontrada", "AUTHORIZATION_NOT_FOUND")))
                .flatMap(authorization -> {
                    Map<String, Object> verificationData = buildVerificationData(authorization);
                    ApiResponse<Map<String, Object>> response =
                            ApiResponse.success(verificationData, "Datos de autorización obtenidos");
                    return ServerResponse.ok()
                            .contentType(APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * Genera y devuelve la imagen QR como PNG.
     * Utilizado para compartir la autorización con el visitante.
     *
     * @param request ServerRequest con path variable "id"
     * @return ServerResponse con imagen PNG del QR
     */
    public Mono<ServerResponse> getQrImage(ServerRequest request) {
        Long authorizationId = Long.parseLong(request.pathVariable("id"));

        return authorizationRepository.findById(authorizationId)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "Autorización no encontrada", "AUTHORIZATION_NOT_FOUND")))
                .flatMap(authorization -> {
                    if (authorization.getSignedQr() == null || authorization.getSignedQr().isBlank()) {
                        return Mono.error(new BusinessException(
                                "La autorización no tiene QR generado", "NO_QR_AVAILABLE"));
                    }
                    return qrImageGeneratorGateway.generateQrImage(authorization.getSignedQr(), 300, 300);
                })
                .flatMap(qrBytes -> ServerResponse.ok()
                        .contentType(IMAGE_PNG)
                        .header("Content-Disposition",
                                "inline; filename=\"authorization-qr-" + authorizationId + ".png\"")
                        .bodyValue(qrBytes))
                .onErrorResume(this::handleError);
    }

    // ─── Private Helpers ────────────────────────────────────────────────────

    private Map<String, Object> buildVerificationData(VisitorAuthorization authorization) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", authorization.getId());
        data.put("personName", authorization.getPersonName());
        data.put("personDocument", authorization.getPersonDocument());
        data.put("serviceType", authorization.getServiceType().name());
        data.put("status", authorization.getStatus().name());
        data.put("validFrom", authorization.getValidFrom().toString());
        data.put("validTo", authorization.getValidTo().toString());

        if (authorization.getVehiclePlate() != null) {
            data.put("vehiclePlate", authorization.getVehiclePlate());
            data.put("vehicleType", authorization.getVehicleType());
            data.put("vehicleColor", authorization.getVehicleColor());
        }

        // Verificación de vigencia
        Instant now = Instant.now();
        boolean isCurrentlyValid = authorization.getStatus().name().equals("ACTIVE")
                && now.isAfter(authorization.getValidFrom())
                && now.isBefore(authorization.getValidTo());
        data.put("isCurrentlyValid", isCurrentlyValid);

        if (!isCurrentlyValid) {
            if (authorization.getStatus().name().equals("REVOKED")) {
                data.put("invalidReason", "REVOKED");
            } else if (now.isAfter(authorization.getValidTo())) {
                data.put("invalidReason", "EXPIRED");
            } else if (now.isBefore(authorization.getValidFrom())) {
                data.put("invalidReason", "NOT_YET_VALID");
            }
        }

        return data;
    }

    private Mono<ServerResponse> handleError(Throwable error) {
        Throwable rootCause = error;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        if (rootCause instanceof NotFoundException notFoundEx) {
            log.warn("External authorization - recurso no encontrado: {}", notFoundEx.getMessage());
            ApiResponse<Void> response = ApiResponse.error(404, notFoundEx.getMessage());
            return ServerResponse.status(404)
                    .contentType(APPLICATION_JSON)
                    .bodyValue(response);
        }

        if (rootCause instanceof BusinessException businessEx) {
            log.warn("External authorization - error de negocio: {} - {}",
                    businessEx.getErrorCode(), businessEx.getMessage());
            ApiResponse<Void> response = ApiResponse.error(
                    businessEx.getHttpStatus(), businessEx.getMessage());
            return ServerResponse.status(businessEx.getHttpStatus())
                    .contentType(APPLICATION_JSON)
                    .bodyValue(response);
        }

        if (rootCause instanceof NumberFormatException) {
            log.warn("External authorization - ID inválido: {}", rootCause.getMessage());
            ApiResponse<Void> response = ApiResponse.error(400, "ID de autorización inválido");
            return ServerResponse.badRequest()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(response);
        }

        log.error("External authorization - error inesperado", error);
        ApiResponse<Void> response = ApiResponse.error(500, "Error interno del servidor");
        return ServerResponse.status(500)
                .contentType(APPLICATION_JSON)
                .bodyValue(response);
    }
}
