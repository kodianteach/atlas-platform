package co.com.atlas.r2dbc.unit;

import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.UnitStatus;
import co.com.atlas.model.unit.UnitType;
import co.com.atlas.model.unit.gateways.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Implementación del gateway UnitRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class UnitRepositoryAdapter implements UnitRepository {

    private final UnitReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Unit> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Mono<Unit> findByOrganizationIdAndCode(Long organizationId, String code) {
        return repository.findByOrganizationIdAndCodeAndDeletedAtIsNull(organizationId, code)
                .map(this::toDomain);
    }

    @Override
    public Flux<Unit> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationIdAndDeletedAtIsNull(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Unit> findByTowerId(Long towerId) {
        return repository.findByTowerIdAndDeletedAtIsNull(towerId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Unit> findByZoneId(Long zoneId) {
        return repository.findByZoneIdAndDeletedAtIsNull(zoneId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Unit> findByUserId(Long userId) {
        String sql = """
            SELECT u.* FROM unit u
            JOIN user_units uu ON uu.unit_id = u.id
            WHERE uu.user_id = :userId AND u.deleted_at IS NULL
            """;
        
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map((row, metadata) -> UnitEntity.builder()
                        .id(row.get("id", Long.class))
                        .organizationId(row.get("organization_id", Long.class))
                        .zoneId(row.get("zone_id", Long.class))
                        .towerId(row.get("tower_id", Long.class))
                        .code(row.get("code", String.class))
                        .type(row.get("type", String.class))
                        .floor(row.get("floor", Integer.class))
                        .areaSqm(row.get("area_sqm", BigDecimal.class))
                        .bedrooms(row.get("bedrooms", Integer.class))
                        .bathrooms(row.get("bathrooms", Integer.class))
                        .parkingSpots(row.get("parking_spots", Integer.class))
                        .status(row.get("status", String.class))
                        .isActive(row.get("is_active", Boolean.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .updatedAt(row.get("updated_at", Instant.class))
                        .build())
                .all()
                .map(this::toDomain);
    }

    @Override
    public Mono<Unit> save(Unit unit) {
        UnitEntity entity = toEntity(unit);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return databaseClient.sql("UPDATE unit SET deleted_at = :now WHERE id = :id")
                .bind("now", Instant.now())
                .bind("id", id)
                .then();
    }

    @Override
    public Mono<Boolean> existsByOrganizationIdAndCode(Long organizationId, String code) {
        return repository.existsByOrganizationIdAndCodeAndDeletedAtIsNull(organizationId, code);
    }

    private Unit toDomain(UnitEntity entity) {
        return Unit.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .zoneId(entity.getZoneId())
                .towerId(entity.getTowerId())
                .code(entity.getCode())
                .type(entity.getType() != null ? UnitType.valueOf(entity.getType()) : null)
                .floor(entity.getFloor())
                .areaSqm(entity.getAreaSqm())
                .bedrooms(entity.getBedrooms())
                .bathrooms(entity.getBathrooms())
                .parkingSpots(entity.getParkingSpots())
                .status(entity.getStatus() != null ? UnitStatus.valueOf(entity.getStatus()) : null)
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private UnitEntity toEntity(Unit unit) {
        return UnitEntity.builder()
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
                .deletedAt(unit.getDeletedAt())
                .build();
    }
}
