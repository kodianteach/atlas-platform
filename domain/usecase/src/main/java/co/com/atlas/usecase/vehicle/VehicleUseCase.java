package co.com.atlas.usecase.vehicle;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.vehicle.BulkInactivateResult;
import co.com.atlas.model.vehicle.BulkSyncResult;
import co.com.atlas.model.vehicle.PlateValidationResult;
import co.com.atlas.model.vehicle.Vehicle;
import co.com.atlas.model.vehicle.VehicleType;
import co.com.atlas.model.vehicle.gateways.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso para gestión de vehículos.
 */
@RequiredArgsConstructor
public class VehicleUseCase {

    private static final int DEFAULT_MAX_VEHICLES = 2;

    private final VehicleRepository vehicleRepository;
    private final UnitRepository unitRepository;

    /**
     * Registra un nuevo vehículo a una vivienda (Unit).
     */
    public Mono<Vehicle> create(Vehicle vehicle) {
        return unitRepository.findById(vehicle.getUnitId())
                .switchIfEmpty(Mono.error(new NotFoundException("Unit", vehicle.getUnitId())))
                .flatMap(unit -> {
                    // Asignar organization_id desde la unidad
                    Vehicle toSave = vehicle.toBuilder()
                            .organizationId(unit.getOrganizationId())
                            .plate(normalizePlate(vehicle.getPlate()))
                            .isActive(vehicle.getIsActive() != null ? vehicle.getIsActive() : true)
                            .build();

                    // Verificar duplicado de placa en la organización
                    return vehicleRepository.findByOrganizationIdAndPlate(unit.getOrganizationId(), toSave.getPlate())
                            .flatMap(existing -> Mono.<Vehicle>error(
                                    new DuplicateException("Vehicle", "plate", toSave.getPlate())))
                            .switchIfEmpty(Mono.defer(() ->
                                    // Verificar cupo máximo de vehículos activos en la unidad
                                    vehicleRepository.countActiveByUnitId(unit.getId())
                                            .flatMap(count -> {
                                                int max = unit.getMaxVehicles() != null
                                                        ? unit.getMaxVehicles()
                                                        : DEFAULT_MAX_VEHICLES;
                                                if (count >= max) {
                                                    return Mono.error(new BusinessException(
                                                            String.format("La unidad %s ya alcanzó el límite de %d vehículos activos",
                                                                    unit.getCode(), max),
                                                            "VEHICLE_LIMIT_EXCEEDED"));
                                                }
                                                return vehicleRepository.save(toSave);
                                            })
                            ));
                });
    }

    /**
     * Obtiene un vehículo por ID.
     */
    public Mono<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Vehicle", id)));
    }

    /**
     * Lista vehículos de una unidad (no eliminados).
     */
    public Flux<Vehicle> findByUnitId(Long unitId) {
        return vehicleRepository.findByUnitId(unitId);
    }

    /**
     * Lista vehículos de una organización con paginación.
     */
    public Mono<PageResponse<Vehicle>> findByOrganizationId(Long organizationId, int page, int size) {
        return vehicleRepository.countByOrganizationId(organizationId)
                .flatMap(total -> vehicleRepository.findByOrganizationId(organizationId, page, size)
                        .collectList()
                        .map(vehicles -> PageResponse.of(vehicles, page, size, total)));
    }

    /**
     * Actualiza un vehículo existente.
     */
    public Mono<Vehicle> update(Long id, Vehicle vehicle) {
        return findById(id)
                .flatMap(existing -> {
                    String newPlate = vehicle.getPlate() != null
                            ? normalizePlate(vehicle.getPlate())
                            : existing.getPlate();

                    // Si cambia la placa, verificar duplicado
                    Mono<Void> plateCheck;
                    if (!newPlate.equals(existing.getPlate())) {
                        plateCheck = vehicleRepository
                                .findByOrganizationIdAndPlate(existing.getOrganizationId(), newPlate)
                                .flatMap(dup -> Mono.<Void>error(
                                        new DuplicateException("Vehicle", "plate", newPlate)))
                                .then();
                    } else {
                        plateCheck = Mono.empty();
                    }

                    return plateCheck.then(Mono.defer(() -> {
                        Vehicle updated = existing.toBuilder()
                                .plate(newPlate)
                                .vehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType() : existing.getVehicleType())
                                .brand(vehicle.getBrand() != null ? vehicle.getBrand() : existing.getBrand())
                                .model(vehicle.getModel() != null ? vehicle.getModel() : existing.getModel())
                                .color(vehicle.getColor() != null ? vehicle.getColor() : existing.getColor())
                                .ownerName(vehicle.getOwnerName() != null ? vehicle.getOwnerName() : existing.getOwnerName())
                                .isActive(vehicle.getIsActive() != null ? vehicle.getIsActive() : existing.getIsActive())
                                .notes(vehicle.getNotes() != null ? vehicle.getNotes() : existing.getNotes())
                                .build();
                        return vehicleRepository.save(updated);
                    }));
                });
    }

    /**
     * Elimina un vehículo (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(existing -> vehicleRepository.delete(id));
    }

    /**
     * Valida si una placa está registrada y activa en una organización (API de guarda).
     */
    public Mono<PlateValidationResult> validatePlate(Long organizationId, String plate) {
        String normalizedPlate = normalizePlate(plate);
        return vehicleRepository.findByOrganizationIdAndPlate(organizationId, normalizedPlate)
                .flatMap(vehicle -> {
                    if (!Boolean.TRUE.equals(vehicle.getIsActive())) {
                        return Mono.just(PlateValidationResult.builder()
                                .allowed(false)
                                .plate(normalizedPlate)
                                .message("Vehículo registrado pero INACTIVO")
                                .build());
                    }
                    // Obtener info de la unidad para la respuesta
                    return unitRepository.findById(vehicle.getUnitId())
                            .map(unit -> PlateValidationResult.builder()
                                    .allowed(true)
                                    .plate(normalizedPlate)
                                    .unitCode(unit.getCode())
                                    .vehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null)
                                    .ownerName(vehicle.getOwnerName())
                                    .message("Vehículo autorizado")
                                    .build())
                            .switchIfEmpty(Mono.just(PlateValidationResult.builder()
                                    .allowed(true)
                                    .plate(normalizedPlate)
                                    .vehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null)
                                    .ownerName(vehicle.getOwnerName())
                                    .message("Vehículo autorizado (unidad no encontrada)")
                                    .build()));
                })
                .switchIfEmpty(Mono.just(PlateValidationResult.builder()
                        .allowed(false)
                        .plate(normalizedPlate)
                        .message("Placa no registrada en esta organización")
                        .build()));
    }

    /**
     * Inactiva todos los vehículos activos de una unidad.
     */
    public Mono<BulkInactivateResult> bulkInactivateByUnit(Long unitId) {
        return unitRepository.findById(unitId)
                .switchIfEmpty(Mono.error(new NotFoundException("Unit", unitId)))
                .flatMap(unit -> vehicleRepository.inactivateAllByUnitId(unitId)
                        .map(count -> BulkInactivateResult.builder()
                                .unitId(unitId)
                                .inactivatedCount(count.intValue())
                                .message(String.format("Se inactivaron %d vehículos de la unidad %s", count, unit.getCode()))
                                .build()));
    }

    /**
     * Sincronización masiva de vehículos para una unidad.
     * Recibe la lista completa deseada: crea nuevos, actualiza existentes, soft-delete los omitidos.
     */
    public Mono<BulkSyncResult> bulkSyncByUnit(Long unitId, List<Vehicle> desiredVehicles) {
        return unitRepository.findById(unitId)
                .switchIfEmpty(Mono.error(new NotFoundException("Unit", unitId)))
                .flatMap(unit -> {
                    int max = unit.getMaxVehicles() != null ? unit.getMaxVehicles() : DEFAULT_MAX_VEHICLES;
                    long activeDesired = desiredVehicles.stream()
                            .filter(v -> v.getIsActive() == null || Boolean.TRUE.equals(v.getIsActive()))
                            .count();
                    if (activeDesired > max) {
                        return Mono.error(new BusinessException(
                                String.format("La sincronización excede el límite de %d vehículos activos para la unidad %s",
                                        max, unit.getCode()),
                                "VEHICLE_LIMIT_EXCEEDED"));
                    }

                    return vehicleRepository.findByUnitId(unitId)
                            .collectList()
                            .flatMap(existingList -> {
                                // Mapa de placa → vehículo existente
                                Map<String, Vehicle> existingMap = existingList.stream()
                                        .collect(Collectors.toMap(Vehicle::getPlate, Function.identity()));

                                // Placas deseadas normalizadas
                                Map<String, Vehicle> desiredMap = desiredVehicles.stream()
                                        .collect(Collectors.toMap(
                                                v -> normalizePlate(v.getPlate()),
                                                Function.identity(),
                                                (a, b) -> b));

                                // Crear nuevos
                                Flux<Vehicle> toCreate = Flux.fromIterable(desiredMap.entrySet())
                                        .filter(e -> !existingMap.containsKey(e.getKey()))
                                        .flatMap(e -> {
                                            Vehicle v = e.getValue().toBuilder()
                                                    .unitId(unitId)
                                                    .organizationId(unit.getOrganizationId())
                                                    .plate(e.getKey())
                                                    .isActive(e.getValue().getIsActive() != null ? e.getValue().getIsActive() : true)
                                                    .build();
                                            return vehicleRepository.save(v);
                                        });

                                // Actualizar existentes que siguen en la lista
                                Flux<Vehicle> toUpdate = Flux.fromIterable(desiredMap.entrySet())
                                        .filter(e -> existingMap.containsKey(e.getKey()))
                                        .flatMap(e -> {
                                            Vehicle existing = existingMap.get(e.getKey());
                                            Vehicle desired = e.getValue();
                                            Vehicle updated = existing.toBuilder()
                                                    .vehicleType(desired.getVehicleType() != null ? desired.getVehicleType() : existing.getVehicleType())
                                                    .brand(desired.getBrand() != null ? desired.getBrand() : existing.getBrand())
                                                    .model(desired.getModel() != null ? desired.getModel() : existing.getModel())
                                                    .color(desired.getColor() != null ? desired.getColor() : existing.getColor())
                                                    .ownerName(desired.getOwnerName() != null ? desired.getOwnerName() : existing.getOwnerName())
                                                    .isActive(desired.getIsActive() != null ? desired.getIsActive() : existing.getIsActive())
                                                    .notes(desired.getNotes() != null ? desired.getNotes() : existing.getNotes())
                                                    .build();
                                            return vehicleRepository.save(updated);
                                        });

                                // Soft-delete los que ya no están en la lista
                                Flux<Void> toDelete = Flux.fromIterable(existingMap.entrySet())
                                        .filter(e -> !desiredMap.containsKey(e.getKey()))
                                        .flatMap(e -> vehicleRepository.delete(e.getValue().getId()));

                                return toCreate.collectList()
                                        .zipWith(toUpdate.collectList())
                                        .flatMap(tuple -> toDelete.then(Mono.just(tuple)))
                                        .flatMap(tuple -> vehicleRepository.findByUnitId(unitId)
                                                .collectList()
                                                .map(finalList -> BulkSyncResult.builder()
                                                        .unitId(unitId)
                                                        .created(tuple.getT1().size())
                                                        .updated(tuple.getT2().size())
                                                        .deleted((int) existingMap.entrySet().stream()
                                                                .filter(e -> !desiredMap.containsKey(e.getKey()))
                                                                .count())
                                                        .vehicles(finalList)
                                                        .message(String.format("Sincronización completa para unidad %s", unit.getCode()))
                                                        .build()));
                            });
                });
    }

    /**
     * Normaliza la placa: mayúsculas, sin espacios.
     */
    private String normalizePlate(String plate) {
        if (plate == null) return null;
        return plate.toUpperCase().trim().replaceAll("\\s+", "");
    }
}
