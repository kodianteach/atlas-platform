package co.com.atlas.r2dbc.organizationconfiguration;

import co.com.atlas.model.organization.OrganizationConfiguration;
import co.com.atlas.model.organization.gateways.OrganizationConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway OrganizationConfigurationRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class OrganizationConfigurationRepositoryAdapter implements OrganizationConfigurationRepository {

    private final OrganizationConfigurationReactiveRepository repository;

    @Override
    public Mono<OrganizationConfiguration> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Mono<OrganizationConfiguration> save(OrganizationConfiguration configuration) {
        OrganizationConfigurationEntity entity = toEntity(configuration);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    private OrganizationConfiguration toDomain(OrganizationConfigurationEntity entity) {
        return OrganizationConfiguration.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .maxUnitsPerDistribution(entity.getMaxUnitsPerDistribution())
                .enableOwnerPermissionManagement(entity.getEnableOwnerPermissionManagement())
                .logoData(entity.getLogoData())
                .logoContentType(entity.getLogoContentType())
                .dominantColor(entity.getDominantColor())
                .secondaryColor(entity.getSecondaryColor())
                .accentColor(entity.getAccentColor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private OrganizationConfigurationEntity toEntity(OrganizationConfiguration domain) {
        return OrganizationConfigurationEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .maxUnitsPerDistribution(domain.getMaxUnitsPerDistribution())
                .enableOwnerPermissionManagement(domain.getEnableOwnerPermissionManagement())
                .logoData(domain.getLogoData())
                .logoContentType(domain.getLogoContentType())
                .dominantColor(domain.getDominantColor())
                .secondaryColor(domain.getSecondaryColor())
                .accentColor(domain.getAccentColor())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
