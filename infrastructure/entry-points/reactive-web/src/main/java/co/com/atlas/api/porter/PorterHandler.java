package co.com.atlas.api.porter;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.porter.dto.CreatePorterRequest;
import co.com.atlas.api.porter.dto.PorterResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterType;
import co.com.atlas.tenant.TenantContext;
import co.com.atlas.usecase.porter.CreatePorterUseCase;
import co.com.atlas.usecase.porter.ListPortersByOrganizationUseCase;
import co.com.atlas.usecase.porter.RegeneratePorterEnrollmentUrlUseCase;
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
 * Handler para operaciones CRUD de porteros.
 * Gestiona la creación, listado y regeneración de URLs de enrolamiento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PorterHandler {

    private final CreatePorterUseCase createPorterUseCase;
    private final ListPortersByOrganizationUseCase listPortersByOrganizationUseCase;
    private final RegeneratePorterEnrollmentUrlUseCase regeneratePorterEnrollmentUrlUseCase;

    /**
     * Crea un nuevo portero.
     *
     * POST /api/porters
     */
    public Mono<ServerResponse> createPorter(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();
            Long adminUserId = TenantContext.getUserIdOrThrow();

            return request.bodyToMono(CreatePorterRequest.class)
                .flatMap(req -> {
                    PorterType type;
                    try {
                        type = PorterType.valueOf(req.getPorterType());
                    } catch (IllegalArgumentException | NullPointerException e) {
                        return Mono.error(new BusinessException(
                                "Tipo de portero inválido. Valores válidos: PORTERO_GENERAL, PORTERO_DELIVERY",
                                "INVALID_PORTER_TYPE"));
                    }

                    CreatePorterUseCase.CreatePorterCommand command =
                            new CreatePorterUseCase.CreatePorterCommand(
                                    req.getDisplayName(),
                                    type
                            );

                    return createPorterUseCase.execute(command, organizationId, adminUserId);
                })
                .flatMap(result -> {
                    PorterResponse data = toPorterResponse(result.porter());
                    data.setEnrollmentUrl(result.enrollmentUrl());

                    ApiResponse<PorterResponse> response = ApiResponse.<PorterResponse>builder()
                            .success(true)
                            .status(HttpStatus.CREATED.value())
                            .message("Portero creado exitosamente")
                            .data(data)
                            .build();

                    return ServerResponse.status(HttpStatus.CREATED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
        }).onErrorResume(this::handleError);
    }

    /**
     * Lista todos los porteros de la organización actual.
     *
     * GET /api/porters
     */
    public Mono<ServerResponse> listPorters(ServerRequest request) {
        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();

            return listPortersByOrganizationUseCase.execute(organizationId)
                .map(this::toPorterResponse)
                .collectList()
                .flatMap(porters -> {
                    ApiResponse<Object> response = ApiResponse.builder()
                            .success(true)
                            .status(HttpStatus.OK.value())
                            .message("Porteros listados exitosamente")
                            .data(porters)
                            .build();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
        }).onErrorResume(this::handleError);
    }

    /**
     * Regenera la URL de enrolamiento de un portero.
     *
     * POST /api/porters/{id}/regenerate-url
     */
    public Mono<ServerResponse> regenerateEnrollmentUrl(ServerRequest request) {
        Long porterId;
        try {
            porterId = Long.parseLong(request.pathVariable("id"));
        } catch (NumberFormatException e) {
            ApiResponse<Void> error = ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "ID de portero inválido"
            );
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(error);
        }

        return Mono.defer(() -> {
            Long organizationId = TenantContext.getOrganizationIdOrThrow();
            Long adminUserId = TenantContext.getUserIdOrThrow();

            return regeneratePorterEnrollmentUrlUseCase.execute(porterId, organizationId, adminUserId)
                .flatMap(result -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("porterId", porterId);
                    data.put("enrollmentUrl", result.enrollmentUrl());

                    ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .status(HttpStatus.OK.value())
                            .message("URL de enrolamiento regenerada exitosamente")
                            .data(data)
                            .build();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
        }).onErrorResume(this::handleError);
    }

    /**
     * Convierte un Porter de dominio a PorterResponse DTO.
     */
    private PorterResponse toPorterResponse(Porter porter) {
        return PorterResponse.builder()
                .id(porter.getId())
                .organizationId(porter.getOrganizationId())
                .names(porter.getNames())
                .email(porter.getEmail())
                .porterType(porter.getPorterType() != null ? porter.getPorterType().name() : null)
                .status(porter.getStatus())
                .createdAt(porter.getCreatedAt())
                .build();
    }

    /**
     * Manejo centralizado de errores.
     */
    private Mono<ServerResponse> handleError(Throwable e) {
        if (e instanceof IllegalStateException && e.getMessage() != null
                && e.getMessage().contains("TenantContext")) {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Sesión inválida: debe iniciar sesión y seleccionar una organización"
            );
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }

        if (e instanceof NotFoundException) {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    e.getMessage()
            );
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }

        if (e instanceof BusinessException) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("errorCode", ((BusinessException) e).getErrorCode());

            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    null,
                    metadata
            );
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }

        log.error("Error inesperado en PorterHandler", e);
        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor"
        );
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
