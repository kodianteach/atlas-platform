package co.com.atlas.r2dbc.tower;

import co.com.atlas.model.tower.Tower;
import co.com.atlas.model.tower.gateways.TowerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway TowerRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class TowerRepositoryAdapter implements TowerRepository {

    private final TowerReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Tower> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Mono<Tower> findByZoneIdAndCode(Long zoneId, String code) {
        return repository.findByZoneIdAndCodeAndDeletedAtIsNull(zoneId, code)
                .map(this::toDomain);
    }

    @Override
    public Flux<Tower> findByZoneId(Long zoneId) {
        return repository.findByZoneIdAndDeletedAtIsNullOrderBySortOrder(zoneId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Tower> findActiveByZoneId(Long zoneId) {
        return repository.findByZoneIdAndIsActiveTrueAndDeletedAtIsNullOrderBySortOrder(zoneId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Tower> findByOrganizationId(Long organizationId) {
        String sql = """
            SELECT t.* FROM tower t
            JOIN zone z ON z.id = t.zone_id
            WHERE z.organization_id = :organizationId 
            AND t.deleted_at IS NULL
            ORDER BY t.sort_order
            """;
        
        return databaseClient.sql(sql)
                .bind("organizationId", organizationId)
                .map((row, metadata) -> TowerEntity.builder()
                        .id(row.get("id", Long.class))
                        .zoneId(row.get("zone_id", Long.class))
                        .code(row.get("code", String.class))
                        .name(row.get("name", String.class))
                        .floorsCount(row.get("floors_count", Integer.class))
                        .description(row.get("description", String.class))
                        .sortOrder(row.get("sort_order", Integer.class))
                        .isActive(row.get("is_active", Boolean.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .updatedAt(row.get("updated_at", Instant.class))
                        .build())
                .all()
                .map(this::toDomain);
    }

    @Override
    public Mono<Tower> save(Tower tower) {
        TowerEntity entity = toEntity(tower);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return databaseClient.sql("UPDATE tower SET deleted_at = :now WHERE id = :id")
                .bind("now", Instant.now())
                .bind("id", id)
                .then();
    }

    @Override
    public Mono<Boolean> existsByZoneIdAndCode(Long zoneId, String code) {
        return repository.existsByZoneIdAndCodeAndDeletedAtIsNull(zoneId, code);
    }

    private Tower toDomain(TowerEntity entity) {
        return Tower.builder()
                .id(entity.getId())
                .zoneId(entity.getZoneId())
                .code(entity.getCode())
                .name(entity.getName())
                .floorsCount(entity.getFloorsCount())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private TowerEntity toEntity(Tower tower) {
        return TowerEntity.builder()
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
                .deletedAt(tower.getDeletedAt())
                .build();
    }
}
