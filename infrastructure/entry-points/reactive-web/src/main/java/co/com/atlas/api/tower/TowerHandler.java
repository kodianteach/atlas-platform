package co.com.atlas.api.tower;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.tower.dto.TowerRequest;
import co.com.atlas.api.tower.dto.TowerResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.tower.Tower;
import co.com.atlas.usecase.tower.TowerUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para operaciones de Tower.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TowerHandler {

    private final TowerUseCase towerUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(TowerRequest.class)
                .flatMap(req -> {
                    Tower tower = Tower.builder()
                            .zoneId(req.getZoneId())
                            .code(req.getCode())
                            .name(req.getName())
                            .floorsCount(req.getFloorsCount())
                            .description(req.getDescription())
                            .sortOrder(req.getSortOrder())
                            .build();
                    return towerUseCase.create(tower);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return towerUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getByZone(ServerRequest request) {
        Long zoneId = Long.parseLong(request.pathVariable("zoneId"));
        return towerUseCase.findByZoneId(zoneId)
                .map(this::toResponse)
                .collectList()
                .flatMap(towers -> {
                    ApiResponse<Object> response = ApiResponse.success(towers, "Torres obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> getByOrganization(ServerRequest request) {
        Long organizationId = Long.parseLong(request.pathVariable("organizationId"));
        return towerUseCase.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(towers -> {
                    ApiResponse<Object> response = ApiResponse.success(towers, "Torres obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return request.bodyToMono(TowerRequest.class)
                .flatMap(req -> {
                    Tower tower = Tower.builder()
                            .id(id)
                            .zoneId(req.getZoneId())
                            .code(req.getCode())
                            .name(req.getName())
                            .floorsCount(req.getFloorsCount())
                            .description(req.getDescription())
                            .sortOrder(req.getSortOrder())
                            .build();
                    return towerUseCase.update(id, tower);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return towerUseCase.delete(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Torre eliminada");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    private Mono<ServerResponse> buildSuccessResponse(Tower tower) {
        TowerResponse data = toResponse(tower);
        ApiResponse<TowerResponse> response = ApiResponse.success(data, "Operación exitosa");
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

    private TowerResponse toResponse(Tower tower) {
        return TowerResponse.builder()
                .id(tower.getId())
                .zoneId(tower.getZoneId())
                .code(tower.getCode())
                .name(tower.getName())
                .floorsCount(tower.getFloorsCount())
                .description(tower.getDescription())
                .sortOrder(tower.getSortOrder())
                .isActive(tower.getIsActive())
                .createdAt(tower.getCreatedAt())
                .updatedAt(tower.getUpdatedAt())
                .build();
    }
}
