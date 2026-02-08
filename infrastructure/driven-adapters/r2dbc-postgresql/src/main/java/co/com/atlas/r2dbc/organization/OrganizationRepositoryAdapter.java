package co.com.atlas.r2dbc.organization;

import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.OrganizationType;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway OrganizationRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final OrganizationReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Organization> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Mono<Organization> findByCode(String code) {
        return repository.findByCodeAndDeletedAtIsNull(code)
                .map(this::toDomain);
    }

    @Override
    public Mono<Organization> findBySlug(String slug) {
        return repository.findBySlugAndDeletedAtIsNull(slug)
                .map(this::toDomain);
    }

    @Override
    public Flux<Organization> findByCompanyId(Long companyId) {
        return repository.findByCompanyIdAndDeletedAtIsNull(companyId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Organization> findAllActive() {
        return repository.findByIsActiveTrueAndDeletedAtIsNull()
                .map(this::toDomain);
    }

    @Override
    public Flux<Organization> findByUserId(Long userId) {
        String sql = """
            SELECT o.* FROM organization o
            JOIN user_organizations uo ON uo.organization_id = o.id
            WHERE uo.user_id = :userId AND o.deleted_at IS NULL
            """;
        
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map((row, metadata) -> OrganizationEntity.builder()
                        .id(row.get("id", Long.class))
                        .companyId(row.get("company_id", Long.class))
                        .code(row.get("code", String.class))
                        .name(row.get("name", String.class))
                        .slug(row.get("slug", String.class))
                        .type(row.get("type", String.class))
                        .usesZones(row.get("uses_zones", Boolean.class))
                        .description(row.get("description", String.class))
                        .settings(row.get("settings", String.class))
                        .status(row.get("status", String.class))
                        .isActive(row.get("is_active", Boolean.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .updatedAt(row.get("updated_at", Instant.class))
                        .build())
                .all()
                .map(this::toDomain);
    }

    @Override
    public Mono<Organization> save(Organization organization) {
        OrganizationEntity entity = toEntity(organization);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return databaseClient.sql("UPDATE organization SET deleted_at = :now WHERE id = :id")
                .bind("now", Instant.now())
                .bind("id", id)
                .then();
    }

    @Override
    public Mono<Boolean> existsByCode(String code) {
        return repository.existsByCodeAndDeletedAtIsNull(code);
    }

    private Organization toDomain(OrganizationEntity entity) {
        return Organization.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .code(entity.getCode())
                .name(entity.getName())
                .slug(entity.getSlug())
                .type(entity.getType() != null ? OrganizationType.valueOf(entity.getType()) : null)
                .usesZones(entity.getUsesZones())
                .description(entity.getDescription())
                .settings(entity.getSettings())
                .status(entity.getStatus())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private OrganizationEntity toEntity(Organization organization) {
        return OrganizationEntity.builder()
                .id(organization.getId())
                .companyId(organization.getCompanyId())
                .code(organization.getCode())
                .name(organization.getName())
                .slug(organization.getSlug())
                .type(organization.getType() != null ? organization.getType().name() : null)
                .usesZones(organization.getUsesZones())
                .description(organization.getDescription())
                .settings(organization.getSettings())
                .status(organization.getStatus())
                .isActive(organization.getIsActive())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .deletedAt(organization.getDeletedAt())
                .build();
    }
}
