package co.com.atlas.usecase.organization;

import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.OrganizationConfiguration;
import co.com.atlas.model.organization.gateways.OrganizationConfigurationRepository;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para gestión de configuración de organización.
 * Permite leer y actualizar los parámetros configurables de una organización.
 */
@RequiredArgsConstructor
public class OrganizationSettingsUseCase {

    private static final System.Logger LOGGER = System.getLogger(OrganizationSettingsUseCase.class.getName());

    private final OrganizationRepository organizationRepository;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;

    /**
     * Obtiene la configuración de una organización.
     * Si no tiene configuración personalizada, retorna valores por defecto.
     *
     * @param organizationId ID de la organización
     * @return configuración de la organización
     */
    public Mono<OrganizationConfiguration> getSettings(Long organizationId) {
        return organizationRepository.findById(organizationId)
            .switchIfEmpty(Mono.error(new NotFoundException("Organization", organizationId)))
            .flatMap(org -> organizationConfigurationRepository.findByOrganizationId(organizationId)
                .defaultIfEmpty(OrganizationConfiguration.builder()
                    .organizationId(organizationId)
                    .maxUnitsPerDistribution(OrganizationConfiguration.DEFAULT_MAX_UNITS_PER_DISTRIBUTION)
                    .build()));
    }

    /**
     * Actualiza la configuración de una organización.
     *
     * @param organizationId ID de la organización
     * @param configuration nueva configuración
     * @return configuración actualizada
     */
    public Mono<OrganizationConfiguration> updateSettings(Long organizationId, OrganizationConfiguration configuration) {
        return organizationRepository.findById(organizationId)
            .switchIfEmpty(Mono.error(new NotFoundException("Organization", organizationId)))
            .flatMap(org -> organizationConfigurationRepository.findByOrganizationId(organizationId)
                .map(existing -> existing.toBuilder()
                    .maxUnitsPerDistribution(configuration.getMaxUnitsPerDistributionOrDefault())
                    .enableOwnerPermissionManagement(configuration.getEnableOwnerPermissionManagement())
                    .build())
                .defaultIfEmpty(OrganizationConfiguration.builder()
                    .organizationId(organizationId)
                    .maxUnitsPerDistribution(configuration.getMaxUnitsPerDistributionOrDefault())
                    .enableOwnerPermissionManagement(configuration.getEnableOwnerPermissionManagement())
                    .build())
                .flatMap(configToSave -> {
                    LOGGER.log(System.Logger.Level.INFO,
                        "Actualizando configuración de organización id={0}: maxUnitsPerDistribution={1}",
                        organizationId, configToSave.getMaxUnitsPerDistributionOrDefault());
                    return organizationConfigurationRepository.save(configToSave);
                }));
    }
}
