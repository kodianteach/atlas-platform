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
                    UnitType unitType = parseUnitType(req.getType());
                    UnitStatus unitStatus = parseUnitStatus(req.getStatus());
                    
                    // Convertir IDs <= 0 a null (son campos opcionales)
                    Long zoneId = req.getZoneId() != null && req.getZoneId() > 0 ? req.getZoneId() : null;
                    Long towerId = req.getTowerId() != null && req.getTowerId() > 0 ? req.getTowerId() : null;
                    Integer floor = req.getFloor() != null && req.getFloor() > 0 ? req.getFloor() : null;
                    
                    Unit unit = Unit.builder()
                            .organizationId(req.getOrganizationId())
                            .zoneId(zoneId)
                            .towerId(towerId)
                            .code(req.getCode())
                            .type(unitType)
                            .floor(floor)
                            .areaSqm(req.getAreaSqm())
                            .bedrooms(req.getBedrooms())
                            .bathrooms(req.getBathrooms())
                            .parkingSpots(req.getParkingSpots())
                            .maxVehicles(req.getMaxVehicles())
                            .status(unitStatus != null ? unitStatus : UnitStatus.AVAILABLE)
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
                    UnitType unitType = parseUnitType(req.getType());
                    UnitStatus unitStatus = parseUnitStatus(req.getStatus());
                    
                    Unit unit = Unit.builder()
                            .id(id)
                            .organizationId(req.getOrganizationId())
                            .zoneId(req.getZoneId())
                            .towerId(req.getTowerId())
                            .code(req.getCode())
                            .type(unitType)
                            .floor(req.getFloor())
                            .areaSqm(req.getAreaSqm())
                            .bedrooms(req.getBedrooms())
                            .bathrooms(req.getBathrooms())
                            .parkingSpots(req.getParkingSpots())
                            .maxVehicles(req.getMaxVehicles())
                            .status(unitStatus)
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
                .maxVehicles(unit.getMaxVehicles())
                .status(unit.getStatus() != null ? unit.getStatus().name() : null)
                .isActive(unit.getIsActive())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }
    
    /**
     * Parsea el tipo de unidad con validación y mensaje de error claro.
     */
    private UnitType parseUnitType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return UnitType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    String.format("Tipo de unidad inválido: '%s'. Valores permitidos: APARTMENT, HOUSE", type),
                    "INVALID_UNIT_TYPE");
        }
    }
    
    /**
     * Parsea el estado de unidad con validación y mensaje de error claro.
     */
    private UnitStatus parseUnitStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UnitStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    String.format("Estado de unidad inválido: '%s'. Valores permitidos: AVAILABLE, OCCUPIED, MAINTENANCE", status),
                    "INVALID_UNIT_STATUS");
        }
    }
}
