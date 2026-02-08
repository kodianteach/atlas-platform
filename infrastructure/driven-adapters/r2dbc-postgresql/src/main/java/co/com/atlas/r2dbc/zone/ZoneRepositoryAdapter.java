package co.com.atlas.r2dbc.zone;

import co.com.atlas.model.zone.Zone;
import co.com.atlas.model.zone.gateways.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway ZoneRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class ZoneRepositoryAdapter implements ZoneRepository {

    private final ZoneReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Zone> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Mono<Zone> findByOrganizationIdAndCode(Long organizationId, String code) {
        return repository.findByOrganizationIdAndCodeAndDeletedAtIsNull(organizationId, code)
                .map(this::toDomain);
    }

    @Override
    public Flux<Zone> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationIdAndDeletedAtIsNullOrderBySortOrder(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Zone> findActiveByOrganizationId(Long organizationId) {
        return repository.findByOrganizationIdAndIsActiveTrueAndDeletedAtIsNullOrderBySortOrder(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Zone> save(Zone zone) {
        ZoneEntity entity = toEntity(zone);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return databaseClient.sql("UPDATE zone SET deleted_at = :now WHERE id = :id")
                .bind("now", Instant.now())
                .bind("id", id)
                .then();
    }

    @Override
    public Mono<Boolean> existsByOrganizationIdAndCode(Long organizationId, String code) {
        return repository.existsByOrganizationIdAndCodeAndDeletedAtIsNull(organizationId, code);
    }

    private Zone toDomain(ZoneEntity entity) {
        return Zone.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private ZoneEntity toEntity(Zone zone) {
        return ZoneEntity.builder()
                .id(zone.getId())
                .organizationId(zone.getOrganizationId())
                .code(zone.getCode())
                .name(zone.getName())
                .description(zone.getDescription())
                .sortOrder(zone.getSortOrder())
                .isActive(zone.getIsActive())
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .deletedAt(zone.getDeletedAt())
                .build();
    }
}
