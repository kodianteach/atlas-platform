package co.com.atlas.api.unit;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.unit.dto.UnitRequest;
import co.com.atlas.api.unit.dto.UnitResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.UnitStatus;
import co.com.atlas.model.unit.UnitType;
import co.com.atlas.usecase.unit.UnitUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para operaciones de Unit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnitHandler {

    private final UnitUseCase unitUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(UnitRequest.class)
                .flatMap(req -> {
                    Unit unit = Unit.builder()
                            .organizationId(req.getOrganizationId())
                            .zoneId(req.getZoneId())
                            .towerId(req.getTowerId())
                            .code(req.getCode())
                            .type(req.getType() != null ? UnitType.valueOf(req.getType()) : null)
                            .floor(req.getFloor())
                            .areaSqm(req.getAreaSqm())
                            .bedrooms(req.getBedrooms())
                            .bathrooms(req.getBathrooms())
                            .parkingSpots(req.getParkingSpots())
                            .status(req.getStatus() != null ? UnitStatus.valueOf(req.getStatus()) : UnitStatus.AVAILABLE)
                            .build();
                    return unitUseCase.create(unit);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return unitUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getByOrganization(ServerRequest request) {
        Long organizationId = Long.parseLong(request.pathVariable("organizationId"));
        return unitUseCase.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(units -> {
                    ApiResponse<Object> response = ApiResponse.success(units, "Unidades obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> getByTower(ServerRequest request) {
        Long towerId = Long.parseLong(request.pathVariable("towerId"));
        return unitUseCase.findByTowerId(towerId)
                .map(this::toResponse)
                .collectList()
                .flatMap(units -> {
                    ApiResponse<Object> response = ApiResponse.success(units, "Unidades obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return request.bodyToMono(UnitRequest.class)
                .flatMap(req -> {
                    Unit unit = Unit.builder()
                            .id(id)
                            .organizationId(req.getOrganizationId())
                            .zoneId(req.getZoneId())
                            .towerId(req.getTowerId())
                            .code(req.getCode())
                            .type(req.getType() != null ? UnitType.valueOf(req.getType()) : null)
                            .floor(req.getFloor())
                            .areaSqm(req.getAreaSqm())
                            .bedrooms(req.getBedrooms())
                            .bathrooms(req.getBathrooms())
                            .parkingSpots(req.getParkingSpots())
                            .status(req.getStatus() != null ? UnitStatus.valueOf(req.getStatus()) : null)
                            .build();
                    return unitUseCase.update(id, unit);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return unitUseCase.delete(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Unidad eliminada");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    private Mono<ServerResponse> buildSuccessResponse(Unit unit) {
        UnitResponse data = toResponse(unit);
        ApiResponse<UnitResponse> response = ApiResponse.success(data, "Operación exitosa");
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

    private UnitResponse toResponse(Unit unit) {
        return UnitResponse.builder()
                .id(unit.getId())
                .organizationId(unit.getOrganizationId())
                .zoneId(unit.getZoneId())
                .towerId(unit.getTowerId())
                .code(unit.getCode())
                .type(unit.getType() != null ? unit.getType().name() : null)
                .floor(unit.getFloor())
                .areaSqm(unit.getAreaSqm())
                .bedrooms(unit.getBedrooms())
                .bathrooms(unit.getBathrooms())
                .parkingSpots(unit.getParkingSpots())
                .status(unit.getStatus() != null ? unit.getStatus().name() : null)
                .isActive(unit.getIsActive())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }
}
