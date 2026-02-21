package co.com.atlas.model.organization.gateways;

import co.com.atlas.model.organization.OrganizationConfiguration;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de configuración de organización.
 */
public interface OrganizationConfigurationRepository {

    /**
     * Busca la configuración de una organización.
     */
    Mono<OrganizationConfiguration> findByOrganizationId(Long organizationId);

    /**
     * Guarda o actualiza la configuración de una organización.
     */
    Mono<OrganizationConfiguration> save(OrganizationConfiguration configuration);
}
