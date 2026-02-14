package co.com.atlas.api.vehicle;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.vehicle.dto.*;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.vehicle.BulkInactivateResult;
import co.com.atlas.model.vehicle.BulkSyncResult;
import co.com.atlas.model.vehicle.PlateValidationResult;
import co.com.atlas.model.vehicle.Vehicle;
import co.com.atlas.model.vehicle.VehicleType;
import co.com.atlas.usecase.vehicle.VehicleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler para operaciones de Vehicle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleHandler {

    private final VehicleUseCase vehicleUseCase;

    // ===================== CRUD =====================

    /**
     * POST /api/vehicles — Registrar vehículo.
     */
    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(VehicleRequest.class)
                .flatMap(req -> {
                    Vehicle vehicle = toDomain(req);
                    return vehicleUseCase.create(vehicle);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(DuplicateException.class, e -> buildErrorResponse(e, HttpStatus.CONFLICT, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    /**
     * GET /api/vehicles/{id} — Obtener vehículo por ID.
     */
    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return vehicleUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    /**
     * GET /api/vehicles/unit/{unitId} — Listar vehículos de una unidad.
     */
    public Mono<ServerResponse> getByUnit(ServerRequest request) {
        Long unitId = Long.parseLong(request.pathVariable("unitId"));
        return vehicleUseCase.findByUnitId(unitId)
                .map(this::toResponse)
                .collectList()
                .flatMap(vehicles -> {
                    ApiResponse<Object> response = ApiResponse.success(vehicles, "Vehículos obtenidos");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    /**
     * GET /api/vehicles/organization/{organizationId}?page=0&size=20 — Listar vehículos paginados.
     */
    public Mono<ServerResponse> getByOrganization(ServerRequest request) {
        Long organizationId = Long.parseLong(request.pathVariable("organizationId"));
        int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(20);

        return vehicleUseCase.findByOrganizationId(organizationId, page, size)
                .map(pageResult -> {
                    List<VehicleResponse> content = pageResult.getContent().stream()
                            .map(this::toResponse)
                            .collect(Collectors.toList());
                    return PageResponse.<VehicleResponse>builder()
                            .content(content)
                            .page(pageResult.getPage())
                            .size(pageResult.getSize())
                            .totalElements(pageResult.getTotalElements())
                            .totalPages(pageResult.getTotalPages())
                            .build();
                })
                .flatMap(pageResponse -> {
                    ApiResponse<Object> response = ApiResponse.success(pageResponse, "Vehículos obtenidos");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    /**
     * PUT /api/vehicles/{id} — Actualizar vehículo.
     */
    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return request.bodyToMono(VehicleRequest.class)
                .flatMap(req -> {
                    Vehicle vehicle = toDomain(req);
                    return vehicleUseCase.update(id, vehicle);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(DuplicateException.class, e -> buildErrorResponse(e, HttpStatus.CONFLICT, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    /**
     * DELETE /api/vehicles/{id} — Eliminar vehículo (soft delete).
     */
    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return vehicleUseCase.delete(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Vehículo eliminado");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    // ===================== Guard / Validate =====================

    /**
     * GET /api/vehicles/validate/{plate}?organizationId=X — Validar placa (API de guarda).
     */
    public Mono<ServerResponse> validatePlate(ServerRequest request) {
        String plate = request.pathVariable("plate");
        Long organizationId = request.queryParam("organizationId")
                .map(Long::parseLong)
                .orElseThrow(() -> new BusinessException("organizationId es requerido", "MISSING_PARAM"));

        return vehicleUseCase.validatePlate(organizationId, plate)
                .map(this::toPlateResponse)
                .flatMap(result -> {
                    ApiResponse<PlateValidationResponse> response = ApiResponse.success(result,
                            result.isAllowed() ? "Acceso permitido" : "Acceso denegado");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    // ===================== Bulk Operations =====================

    /**
     * PATCH /api/vehicles/unit/{unitId}/inactivate — Inactivar todos los vehículos de una unidad.
     */
    public Mono<ServerResponse> bulkInactivate(ServerRequest request) {
        Long unitId = Long.parseLong(request.pathVariable("unitId"));
        return vehicleUseCase.bulkInactivateByUnit(unitId)
                .map(result -> BulkInactivateResponse.builder()
                        .unitId(result.getUnitId())
                        .inactivatedCount(result.getInactivatedCount())
                        .message(result.getMessage())
                        .build())
                .flatMap(result -> {
                    ApiResponse<BulkInactivateResponse> response = ApiResponse.success(result, result.getMessage());
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    /**
     * PUT /api/vehicles/unit/{unitId}/sync — Sincronización masiva de vehículos de una unidad.
     */
    public Mono<ServerResponse> bulkSync(ServerRequest request) {
        Long unitId = Long.parseLong(request.pathVariable("unitId"));
        return request.bodyToMono(BulkSyncRequest.class)
                .flatMap(req -> {
                    List<Vehicle> desired = req.getVehicles().stream()
                            .map(this::toDomain)
                            .collect(Collectors.toList());
                    return vehicleUseCase.bulkSyncByUnit(unitId, desired);
                })
                .map(result -> BulkSyncResponse.builder()
                        .unitId(result.getUnitId())
                        .created(result.getCreated())
                        .updated(result.getUpdated())
                        .deleted(result.getDeleted())
                        .vehicles(result.getVehicles().stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList()))
                        .message(result.getMessage())
                        .build())
                .flatMap(result -> {
                    ApiResponse<BulkSyncResponse> response = ApiResponse.success(result, result.getMessage());
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    // ===================== Mappers =====================

    private Vehicle toDomain(VehicleRequest req) {
        return Vehicle.builder()
                .unitId(req.getUnitId())
                .plate(req.getPlate())
                .vehicleType(parseVehicleType(req.getVehicleType()))
                .brand(req.getBrand())
                .model(req.getModel())
                .color(req.getColor())
                .ownerName(req.getOwnerName())
                .isActive(req.getIsActive())
                .notes(req.getNotes())
                .build();
    }
    
    private VehicleType parseVehicleType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return VehicleType.valueOf(type.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            String validValues = java.util.Arrays.stream(VehicleType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("Tipo de vehículo inválido: '" + type + "'. Valores válidos: " + validValues);
        }
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .unitId(vehicle.getUnitId())
                .organizationId(vehicle.getOrganizationId())
                .plate(vehicle.getPlate())
                .vehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null)
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .ownerName(vehicle.getOwnerName())
                .isActive(vehicle.getIsActive())
                .registeredBy(vehicle.getRegisteredBy())
                .notes(vehicle.getNotes())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    private PlateValidationResponse toPlateResponse(PlateValidationResult result) {
        return PlateValidationResponse.builder()
                .allowed(result.isAllowed())
                .plate(result.getPlate())
                .unitCode(result.getUnitCode())
                .vehicleType(result.getVehicleType())
                .ownerName(result.getOwnerName())
                .message(result.getMessage())
                .build();
    }

    private Mono<ServerResponse> buildSuccessResponse(Vehicle vehicle) {
        VehicleResponse data = toResponse(vehicle);
        ApiResponse<VehicleResponse> response = ApiResponse.success(data, "Operación exitosa");
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
}
