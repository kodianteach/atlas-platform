package co.com.atlas.usecase.organization;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.OrganizationConfiguration;
import co.com.atlas.model.organization.gateways.OrganizationConfigurationRepository;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Caso de uso para gestión de configuración de organización.
 * Permite leer y actualizar los parámetros configurables de una organización.
 */
@RequiredArgsConstructor
public class OrganizationSettingsUseCase {

    private static final System.Logger LOGGER = System.getLogger(OrganizationSettingsUseCase.class.getName());
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg");
    private static final String ENTITY_NAME = "Organization";

    private final OrganizationRepository organizationRepository;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;
    private final long maxLogoSizeBytes;

    /**
     * Obtiene la configuración de una organización.
     * Si no tiene configuración personalizada, retorna valores por defecto.
     *
     * @param organizationId ID de la organización
     * @return configuración de la organización
     */
    public Mono<OrganizationConfiguration> getSettings(Long organizationId) {
        return organizationRepository.findById(organizationId)
            .switchIfEmpty(Mono.error(new NotFoundException(ENTITY_NAME, organizationId)))
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
            .switchIfEmpty(Mono.error(new NotFoundException(ENTITY_NAME, organizationId)))
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

    /**
     * Actualiza el branding de una organización (logo + colores).
     *
     * @param organizationId ID de la organización
     * @param logoData       bytes de la imagen del logo (puede ser null para no cambiar)
     * @param contentType    tipo MIME de la imagen (image/png o image/jpeg)
     * @param dominantColor  color dominante en formato hex (#RRGGBB)
     * @param secondaryColor color secundario en formato hex (#RRGGBB)
     * @param accentColor    color de acento en formato hex (#RRGGBB)
     * @return configuración actualizada
     */
    public Mono<OrganizationConfiguration> updateBranding(Long organizationId,
                                                           byte[] logoData,
                                                           String contentType,
                                                           String dominantColor,
                                                           String secondaryColor,
                                                           String accentColor) {
        return organizationRepository.findById(organizationId)
            .switchIfEmpty(Mono.error(new NotFoundException(ENTITY_NAME, organizationId)))
            .flatMap(org -> {
                validateBranding(logoData, contentType, dominantColor, secondaryColor, accentColor);
                return organizationConfigurationRepository.findByOrganizationId(organizationId)
                    .map(existing -> existing.toBuilder()
                        .logoData(logoData != null ? logoData : existing.getLogoData())
                        .logoContentType(contentType != null ? contentType : existing.getLogoContentType())
                        .dominantColor(dominantColor)
                        .secondaryColor(secondaryColor)
                        .accentColor(accentColor)
                        .build())
                    .defaultIfEmpty(OrganizationConfiguration.builder()
                        .organizationId(organizationId)
                        .maxUnitsPerDistribution(OrganizationConfiguration.DEFAULT_MAX_UNITS_PER_DISTRIBUTION)
                        .logoData(logoData)
                        .logoContentType(contentType)
                        .dominantColor(dominantColor)
                        .secondaryColor(secondaryColor)
                        .accentColor(accentColor)
                        .build())
                    .flatMap(configToSave -> {
                        LOGGER.log(System.Logger.Level.INFO,
                            "Actualizando branding de organización id={0}: dominantColor={1}, secondaryColor={2}, accentColor={3}",
                            organizationId, dominantColor, secondaryColor, accentColor);
                        return organizationConfigurationRepository.save(configToSave);
                    });
            });
    }

    /**
     * Validates branding data before persisting.
     */
    private void validateBranding(byte[] logoData, String contentType,
                                   String dominantColor, String secondaryColor, String accentColor) {
        if (logoData != null) {
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new BusinessException(
                    "Formato de imagen no soportado. Solo se permiten PNG y JPEG",
                    "INVALID_IMAGE_FORMAT");
            }
            if (logoData.length > maxLogoSizeBytes) {
                throw new BusinessException(
                    String.format("La imagen excede el tamaño máximo permitido de %d bytes", maxLogoSizeBytes),
                    "IMAGE_TOO_LARGE");
            }
        }
        if (!OrganizationConfiguration.isValidHexColor(dominantColor)) {
            throw new BusinessException(
                "Color dominante inválido. Use formato hex #RRGGBB",
                "INVALID_COLOR_FORMAT");
        }
        if (!OrganizationConfiguration.isValidHexColor(secondaryColor)) {
            throw new BusinessException(
                "Color secundario inválido. Use formato hex #RRGGBB",
                "INVALID_COLOR_FORMAT");
        }
        if (!OrganizationConfiguration.isValidHexColor(accentColor)) {
            throw new BusinessException(
                "Color de acento inválido. Use formato hex #RRGGBB",
                "INVALID_COLOR_FORMAT");
        }
    }
}
