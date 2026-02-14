package co.com.atlas.r2dbc.vehicle;

import co.com.atlas.model.vehicle.Vehicle;
import co.com.atlas.model.vehicle.VehicleType;
import co.com.atlas.model.vehicle.gateways.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway VehicleRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VehicleReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Vehicle> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Flux<Vehicle> findByUnitId(Long unitId) {
        return repository.findByUnitIdAndDeletedAtIsNull(unitId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Vehicle> findActiveByUnitId(Long unitId) {
        return repository.findByUnitIdAndIsActiveTrueAndDeletedAtIsNull(unitId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Vehicle> findByOrganizationId(Long organizationId, int page, int size) {
        String sql = """
            SELECT * FROM vehicles
            WHERE organization_id = :orgId AND deleted_at IS NULL
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """;
        return databaseClient.sql(sql)
                .bind("orgId", organizationId)
                .bind("limit", size)
                .bind("offset", page * size)
                .map((row, metadata) -> VehicleEntity.builder()
                        .id(row.get("id", Long.class))
                        .unitId(row.get("unit_id", Long.class))
                        .organizationId(row.get("organization_id", Long.class))
                        .plate(row.get("plate", String.class))
                        .vehicleType(row.get("vehicle_type", String.class))
                        .brand(row.get("brand", String.class))
                        .model(row.get("model", String.class))
                        .color(row.get("color", String.class))
                        .ownerName(row.get("owner_name", String.class))
                        .isActive(row.get("is_active", Boolean.class))
                        .registeredBy(row.get("registered_by", Long.class))
                        .notes(row.get("notes", String.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .updatedAt(row.get("updated_at", Instant.class))
                        .build())
                .all()
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countByOrganizationId(Long organizationId) {
        return repository.countByOrganizationIdAndDeletedAtIsNull(organizationId);
    }

    @Override
    public Mono<Vehicle> findByOrganizationIdAndPlate(Long organizationId, String plate) {
        return repository.findByOrganizationIdAndPlateAndDeletedAtIsNull(organizationId, plate)
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countActiveByUnitId(Long unitId) {
        return repository.countByUnitIdAndIsActiveTrueAndDeletedAtIsNull(unitId);
    }

    @Override
    public Mono<Vehicle> save(Vehicle vehicle) {
        VehicleEntity entity = toEntity(vehicle);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return databaseClient.sql("UPDATE vehicles SET deleted_at = :now WHERE id = :id")
                .bind("now", Instant.now())
                .bind("id", id)
                .then();
    }

    @Override
    public Mono<Long> inactivateAllByUnitId(Long unitId) {
        String sql = """
            UPDATE vehicles SET is_active = FALSE, updated_at = :now
            WHERE unit_id = :unitId AND is_active = TRUE AND deleted_at IS NULL
            """;
        return databaseClient.sql(sql)
                .bind("now", Instant.now())
                .bind("unitId", unitId)
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Boolean> existsActiveByOrganizationIdAndPlate(Long organizationId, String plate) {
        return repository.existsByOrganizationIdAndPlateAndIsActiveTrueAndDeletedAtIsNull(organizationId, plate);
    }

    // ===================== Mappers =====================

    private Vehicle toDomain(VehicleEntity entity) {
        return Vehicle.builder()
                .id(entity.getId())
                .unitId(entity.getUnitId())
                .organizationId(entity.getOrganizationId())
                .plate(entity.getPlate())
                .vehicleType(entity.getVehicleType() != null ? VehicleType.valueOf(entity.getVehicleType()) : null)
                .brand(entity.getBrand())
                .model(entity.getModel())
                .color(entity.getColor())
                .ownerName(entity.getOwnerName())
                .isActive(entity.getIsActive())
                .registeredBy(entity.getRegisteredBy())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private VehicleEntity toEntity(Vehicle vehicle) {
        return VehicleEntity.builder()
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
                .deletedAt(vehicle.getDeletedAt())
                .build();
    }
}
