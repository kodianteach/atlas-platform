package co.com.atlas.usecase.organization;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.OrganizationConfiguration;
import co.com.atlas.model.organization.gateways.OrganizationConfigurationRepository;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static co.com.atlas.usecase.organization.builders.OrganizationConfigurationBuilder.anOrganizationConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationSettingsUseCaseTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationConfigurationRepository organizationConfigurationRepository;

    private OrganizationSettingsUseCase useCase;

    private static final Long ORG_ID = 100L;
    private static final long MAX_LOGO_SIZE = 2_097_152L; // 2MB
    private static final byte[] SMALL_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG header

    @BeforeEach
    void setUp() {
        useCase = new OrganizationSettingsUseCase(
                organizationRepository, organizationConfigurationRepository, MAX_LOGO_SIZE);
    }

    // ── updateBranding ──────────────────────────────────────────────────────

    @Test
    void shouldUpdateBrandingSuccessfully() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        OrganizationConfiguration existing = anOrganizationConfig()
                .withOrganizationId(ORG_ID)
                .build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(organizationConfigurationRepository.findByOrganizationId(ORG_ID)).thenReturn(Mono.just(existing));
        when(organizationConfigurationRepository.save(any(OrganizationConfiguration.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(useCase.updateBranding(ORG_ID, SMALL_PNG, "image/png", "#FF8C61", "#4A90D9", "#27AE60"))
                .assertNext(config -> {
                    assertThat(config.getDominantColor()).isEqualTo("#FF8C61");
                    assertThat(config.getSecondaryColor()).isEqualTo("#4A90D9");
                    assertThat(config.getAccentColor()).isEqualTo("#27AE60");
                    assertThat(config.getLogoData()).isEqualTo(SMALL_PNG);
                    assertThat(config.getLogoContentType()).isEqualTo("image/png");
                })
                .verifyComplete();
    }

    @Test
    void shouldUpdateBrandingWithColorsOnlyWhenNoLogo() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        OrganizationConfiguration existing = anOrganizationConfig()
                .withOrganizationId(ORG_ID)
                .build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(organizationConfigurationRepository.findByOrganizationId(ORG_ID)).thenReturn(Mono.just(existing));
        when(organizationConfigurationRepository.save(any(OrganizationConfiguration.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert — null logo should be accepted, only persist colors
        StepVerifier.create(useCase.updateBranding(ORG_ID, null, null, "#FF8C61", "#4A90D9", "#27AE60"))
                .assertNext(config -> {
                    assertThat(config.getDominantColor()).isEqualTo("#FF8C61");
                    assertThat(config.getLogoData()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectInvalidImageFormat() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));

        // Act & Assert
        StepVerifier.create(useCase.updateBranding(ORG_ID, SMALL_PNG, "image/gif", "#FF8C61", "#4A90D9", "#27AE60"))
                .expectErrorMatches(ex -> ex instanceof BusinessException
                        && ex.getMessage().contains("Formato de imagen no soportado")
                        && ((BusinessException) ex).getErrorCode().equals("INVALID_IMAGE_FORMAT"))
                .verify();
    }

    @Test
    void shouldRejectOversizedImage() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        byte[] largeImage = new byte[(int) MAX_LOGO_SIZE + 1];

        // Act & Assert
        StepVerifier.create(useCase.updateBranding(ORG_ID, largeImage, "image/png", "#FF8C61", "#4A90D9", "#27AE60"))
                .expectErrorMatches(ex -> ex instanceof BusinessException
                        && ex.getMessage().contains("tamaño máximo")
                        && ((BusinessException) ex).getErrorCode().equals("IMAGE_TOO_LARGE"))
                .verify();
    }

    @Test
    void shouldRejectInvalidDominantColorHex() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));

        // Act & Assert
        StepVerifier.create(useCase.updateBranding(ORG_ID, null, null, "NOT_HEX", "#4A90D9", "#27AE60"))
                .expectErrorMatches(ex -> ex instanceof BusinessException
                        && ex.getMessage().contains("Color dominante inválido")
                        && ((BusinessException) ex).getErrorCode().equals("INVALID_COLOR_FORMAT"))
                .verify();
    }

    @Test
    void shouldRejectInvalidSecondaryColorHex() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));

        // Act & Assert
        StepVerifier.create(useCase.updateBranding(ORG_ID, null, null, "#FF8C61", "xyz", "#27AE60"))
                .expectErrorMatches(ex -> ex instanceof BusinessException
                        && ex.getMessage().contains("Color secundario inválido"))
                .verify();
    }

    @Test
    void shouldRejectInvalidAccentColorHex() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));

        // Act & Assert
        StepVerifier.create(useCase.updateBranding(ORG_ID, null, null, "#FF8C61", "#4A90D9", "123456"))
                .expectErrorMatches(ex -> ex instanceof BusinessException
                        && ex.getMessage().contains("Color de acento inválido"))
                .verify();
    }

    @Test
    void shouldCreateNewConfigWhenNoneExistsForBranding() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(organizationConfigurationRepository.findByOrganizationId(ORG_ID)).thenReturn(Mono.empty());
        when(organizationConfigurationRepository.save(any(OrganizationConfiguration.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(useCase.updateBranding(ORG_ID, SMALL_PNG, "image/jpeg", "#FF8C61", "#4A90D9", "#27AE60"))
                .assertNext(config -> {
                    assertThat(config.getOrganizationId()).isEqualTo(ORG_ID);
                    assertThat(config.getDominantColor()).isEqualTo("#FF8C61");
                    assertThat(config.getLogoContentType()).isEqualTo("image/jpeg");
                })
                .verifyComplete();
    }

    @Test
    void shouldAcceptNullColorsForBranding() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).build();
        OrganizationConfiguration existing = anOrganizationConfig()
                .withOrganizationId(ORG_ID)
                .withDominantColor("#FF8C61")
                .build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(organizationConfigurationRepository.findByOrganizationId(ORG_ID)).thenReturn(Mono.just(existing));
        when(organizationConfigurationRepository.save(any(OrganizationConfiguration.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert — null colors should clear the branding
        StepVerifier.create(useCase.updateBranding(ORG_ID, null, null, null, null, null))
                .assertNext(config -> {
                    assertThat(config.getDominantColor()).isNull();
                    assertThat(config.getSecondaryColor()).isNull();
                    assertThat(config.getAccentColor()).isNull();
                })
                .verifyComplete();
    }
}
