package co.com.atlas.r2dbc.organizationconfiguration;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para OrganizationConfigurationEntity.
 */
public interface OrganizationConfigurationReactiveRepository
        extends ReactiveCrudRepository<OrganizationConfigurationEntity, Long> {

    /**
     * Busca la configuración por ID de organización.
     */
    Mono<OrganizationConfigurationEntity> findByOrganizationId(Long organizationId);
}
