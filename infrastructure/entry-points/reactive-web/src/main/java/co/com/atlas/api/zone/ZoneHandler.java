package co.com.atlas.api.zone;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.zone.dto.ZoneRequest;
import co.com.atlas.api.zone.dto.ZoneResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.zone.Zone;
import co.com.atlas.usecase.zone.ZoneUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para operaciones de Zone.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ZoneHandler {

    private final ZoneUseCase zoneUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(ZoneRequest.class)
                .flatMap(req -> {
                    Zone zone = Zone.builder()
                            .organizationId(req.getOrganizationId())
                            .code(req.getCode())
                            .name(req.getName())
                            .description(req.getDescription())
                            .sortOrder(req.getSortOrder())
                            .build();
                    return zoneUseCase.create(zone);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return zoneUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getByOrganization(ServerRequest request) {
        Long organizationId = Long.parseLong(request.pathVariable("organizationId"));
        return zoneUseCase.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(zones -> {
                    ApiResponse<Object> response = ApiResponse.success(zones, "Zonas obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return request.bodyToMono(ZoneRequest.class)
                .flatMap(req -> {
                    Zone zone = Zone.builder()
                            .id(id)
                            .organizationId(req.getOrganizationId())
                            .code(req.getCode())
                            .name(req.getName())
                            .description(req.getDescription())
                            .sortOrder(req.getSortOrder())
                            .build();
                    return zoneUseCase.update(id, zone);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return zoneUseCase.delete(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Zona eliminada");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    private Mono<ServerResponse> buildSuccessResponse(Zone zone) {
        ZoneResponse data = toResponse(zone);
        ApiResponse<ZoneResponse> response = ApiResponse.success(data, "Operación exitosa");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

    private Mono<ServerResponse> buildErrorResponse(Exception e, HttpStatus status, String path) {
        ApiResponse<Void> response = ApiResponse.error(status.value(), e.getMessage(), path, null);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

    private ZoneResponse toResponse(Zone zone) {
        return ZoneResponse.builder()
                .id(zone.getId())
                .organizationId(zone.getOrganizationId())
                .code(zone.getCode())
                .name(zone.getName())
                .description(zone.getDescription())
                .sortOrder(zone.getSortOrder())
                .isActive(zone.getIsActive())
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }
}
