package co.com.atlas.r2dbc.company;

import co.com.atlas.model.company.Company;
import co.com.atlas.model.company.gateways.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway CompanyRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {

    private final CompanyReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Company> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Mono<Company> findBySlug(String slug) {
        return repository.findBySlugAndDeletedAtIsNull(slug)
                .map(this::toDomain);
    }

    @Override
    public Flux<Company> findAllActive() {
        return repository.findByIsActiveTrueAndDeletedAtIsNull()
                .map(this::toDomain);
    }

    @Override
    public Mono<Company> save(Company company) {
        CompanyEntity entity = toEntity(company);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return databaseClient.sql("UPDATE company SET deleted_at = :now WHERE id = :id")
                .bind("now", Instant.now())
                .bind("id", id)
                .then();
    }

    @Override
    public Mono<Boolean> existsBySlug(String slug) {
        return repository.existsBySlugAndDeletedAtIsNull(slug);
    }

    private Company toDomain(CompanyEntity entity) {
        return Company.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .taxId(entity.getTaxId())
                .industry(entity.getIndustry())
                .website(entity.getWebsite())
                .address(entity.getAddress())
                .country(entity.getCountry())
                .city(entity.getCity())
                .status(entity.getStatus())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private CompanyEntity toEntity(Company company) {
        return CompanyEntity.builder()
                .id(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .taxId(company.getTaxId())
                .industry(company.getIndustry())
                .website(company.getWebsite())
                .address(company.getAddress())
                .country(company.getCountry())
                .city(company.getCity())
                .status(company.getStatus())
                .isActive(company.getIsActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .deletedAt(company.getDeletedAt())
                .build();
    }
}
