package co.com.atlas.api.porter;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.porter.dto.ValidateAuthorizationRequest;
import co.com.atlas.api.porter.dto.ValidateByDocumentRequest;
import co.com.atlas.api.porter.dto.VehicleExitRequest;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.tenant.TenantContext;
import co.com.atlas.usecase.access.GetRevocationListUseCase;
import co.com.atlas.usecase.access.RegisterVehicleExitUseCase;
import co.com.atlas.usecase.access.SyncAccessEventsUseCase;
import co.com.atlas.usecase.access.ValidateAuthorizationUseCase;
import co.com.atlas.usecase.access.ValidateByDocumentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler para operaciones de validación de acceso en portería.
 * Endpoints autenticados para validar autorizaciones (QR o documento),
 * sincronizar eventos offline, consultar revocaciones y registrar salidas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessPorterHandler {

    private final ValidateAuthorizationUseCase validateAuthorizationUseCase;
    private final ValidateByDocumentUseCase validateByDocumentUseCase;
    private final SyncAccessEventsUseCase syncAccessEventsUseCase;
    private final GetRevocationListUseCase getRevocationListUseCase;
    private final RegisterVehicleExitUseCase registerVehicleExitUseCase;

    /**
     * Valida una autorización online mediante QR firmado.
     * POST /api/porter/validate-authorization
     */
    public Mono<ServerResponse> validateAuthorization(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();
            Long porterUserId = TenantContext.getUserIdOrThrow();
            String deviceId = request.headers().firstHeader("X-Device-Id");

            return request.bodyToMono(ValidateAuthorizationRequest.class)
                    .flatMap(req -> validateAuthorizationUseCase.execute(
                            req.getSignedQr(), porterUserId, deviceId, organizationId))
                    .flatMap(event -> {
                        ApiResponse<AccessEvent> response = ApiResponse.<AccessEvent>builder()
                                .success(true)
                                .status(HttpStatus.OK.value())
                                .message(getMessageForResult(event))
                                .data(event)
                                .build();
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(response);
                    });
        }).onErrorResume(this::handleError);
    }

    /**
     * Busca autorizaciones vigentes por documento de identidad.
     * GET /api/porter/validate-by-document?document={doc}
     */
    public Mono<ServerResponse> findByDocument(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();
            String document = request.queryParam("document").orElse("");

            return validateByDocumentUseCase.findActiveByDocument(document, organizationId)
                    .collectList()
                    .flatMap(authorizations -> {
                        ApiResponse<Object> response = ApiResponse.builder()
                                .success(true)
                                .status(HttpStatus.OK.value())
                                .message(authorizations.isEmpty()
                                        ? "No se encontraron autorizaciones vigentes"
                                        : authorizations.size() + " autorización(es) encontrada(s)")
                                .data(authorizations)
                                .build();
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(response);
                    });
        }).onErrorResume(this::handleError);
    }

    /**
     * Valida y registra acceso por documento (seleccionando una autorización).
     * POST /api/porter/validate-by-document
     */
    public Mono<ServerResponse> validateByDocument(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();
            Long porterUserId = TenantContext.getUserIdOrThrow();
            String deviceId = request.headers().firstHeader("X-Device-Id");

            return request.bodyToMono(ValidateByDocumentRequest.class)
                    .flatMap(req -> validateByDocumentUseCase.validateAndRegister(
                            req.getAuthorizationId(), porterUserId, deviceId, organizationId))
                    .flatMap(event -> {
                        ApiResponse<AccessEvent> response = ApiResponse.<AccessEvent>builder()
                                .success(true)
                                .status(HttpStatus.OK.value())
                                .message(getMessageForResult(event))
                                .data(event)
                                .build();
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(response);
                    });
        }).onErrorResume(this::handleError);
    }

    /**
     * Sincroniza eventos de acceso offline al backend.
     * POST /api/porter/access-events/sync
     */
    public Mono<ServerResponse> syncEvents(ServerRequest request) {
        return Mono.defer(() -> request.bodyToFlux(AccessEvent.class)
                .collectList()
                .flatMap(events -> syncAccessEventsUseCase.execute(events)
                        .collectList()
                        .flatMap(synced -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("syncedCount", synced.size());
                            data.put("syncedIds", synced.stream().map(AccessEvent::getId).toList());

                            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                                    .success(true)
                                    .status(HttpStatus.OK.value())
                                    .message(synced.size() + " evento(s) sincronizado(s)")
                                    .data(data)
                                    .build();
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(response);
                        }))
        ).onErrorResume(this::handleError);
    }

    /**
     * Obtiene la lista de autorizaciones revocadas.
     * GET /api/porter/revocations?since={timestamp}
     */
    public Mono<ServerResponse> getRevocations(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();
            String sinceParam = request.queryParam("since").orElse(null);
            Instant since = sinceParam != null ? Instant.parse(sinceParam) : null;

            return getRevocationListUseCase.execute(organizationId, since)
                    .collectList()
                    .flatMap(revokedIds -> {
                        ApiResponse<List<Long>> response = ApiResponse.<List<Long>>builder()
                                .success(true)
                                .status(HttpStatus.OK.value())
                                .message(revokedIds.size() + " revocación(es)")
                                .data(revokedIds)
                                .build();
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(response);
                    });
        }).onErrorResume(this::handleError);
    }

    /**
     * Registra la salida de un vehículo.
     * POST /api/porter/vehicle-exit
     */
    public Mono<ServerResponse> registerVehicleExit(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();
            Long porterUserId = TenantContext.getUserIdOrThrow();
            String deviceId = request.headers().firstHeader("X-Device-Id");

            return request.bodyToMono(VehicleExitRequest.class)
                    .flatMap(req -> registerVehicleExitUseCase.execute(
                            req.getVehiclePlate(), req.getPersonName(),
                            porterUserId, organizationId, deviceId))
                    .flatMap(event -> {
                        ApiResponse<AccessEvent> response = ApiResponse.<AccessEvent>builder()
                                .success(true)
                                .status(HttpStatus.OK.value())
                                .message("Salida de vehículo registrada")
                                .data(event)
                                .build();
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(response);
                    });
        }).onErrorResume(this::handleError);
    }

    /**
     * Historial de eventos de acceso de la organización.
     * GET /api/porter/access-events
     */
    public Mono<ServerResponse> getAccessEvents(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();

            return validateByDocumentUseCase.getAccessEventsByOrganization(organizationId)
                    .collectList()
                    .flatMap(events -> {
                        ApiResponse<List<AccessEvent>> response = ApiResponse.<List<AccessEvent>>builder()
                                .success(true)
                                .status(HttpStatus.OK.value())
                                .message(events.isEmpty()
                                        ? "No hay eventos de acceso registrados"
                                        : events.size() + " evento(s) de acceso")
                                .data(events)
                                .build();
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(response);
                    });
        }).onErrorResume(this::handleError);
    }

    private String getMessageForResult(AccessEvent event) {
        if (event.getScanResult() == null) {
            return "Evento registrado";
        }
        return switch (event.getScanResult()) {
            case VALID -> "Autorización VÁLIDA — Acceso permitido";
            case INVALID -> "Autorización INVÁLIDA — QR no reconocido";
            case EXPIRED -> "Autorización EXPIRADA o NO VIGENTE";
            case REVOKED -> "Autorización REVOCADA";
            case ALREADY_USED -> "Autorización ya fue utilizada";
        };
    }

    private Mono<ServerResponse> handleError(Throwable e) {
        if (e instanceof IllegalStateException && e.getMessage() != null
                && e.getMessage().contains("TenantContext")) {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Sesión inválida: debe iniciar sesión"
            );
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }
        if (e instanceof NotFoundException) {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(), e.getMessage());
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }
        if (e instanceof BusinessException) {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(), e.getMessage());
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }
        log.error("Error inesperado en AccessPorterHandler", e);
        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor");
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
