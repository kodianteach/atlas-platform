package co.com.atlas.api.organization;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.organization.OrganizationConfiguration;
import co.com.atlas.tenant.TenantContext;
import co.com.atlas.usecase.organization.OrganizationSettingsUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrganizationSettingsHandler.
 * Tests get and update settings with branding support (HU #10).
 */
@ExtendWith(MockitoExtension.class)
class OrganizationSettingsHandlerTest {

    @Mock
    private OrganizationSettingsUseCase organizationSettingsUseCase;

    @InjectMocks
    private OrganizationSettingsHandler handler;

    private static final Long ORG_ID = 100L;

    @BeforeEach
    void setUp() {
        TenantContext.setOrganizationId(ORG_ID);
        TenantContext.setUserId(1L);
        TenantContext.setRoles(List.of("ADMIN_ATLAS"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("GET /api/organization/settings")
    class GetSettingsTests {

        @Test
        @DisplayName("Should return settings with branding colors and logo")
        void shouldReturnSettingsWithBranding() {
            byte[] logoData = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
            OrganizationConfiguration config = OrganizationConfiguration.builder()
                    .organizationId(ORG_ID)
                    .maxUnitsPerDistribution(100)
                    .enableOwnerPermissionManagement(false)
                    .logoData(logoData)
                    .logoContentType("image/png")
                    .dominantColor("#FF8C61")
                    .secondaryColor("#4A90D9")
                    .accentColor("#27AE60")
                    .build();

            when(organizationSettingsUseCase.getSettings(ORG_ID)).thenReturn(Mono.just(config));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/organization/settings"))
                    .build();

            StepVerifier.create(handler.getSettings(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    })
                    .verifyComplete();

            verify(organizationSettingsUseCase).getSettings(ORG_ID);
        }

        @Test
        @DisplayName("Should return settings without branding when not configured")
        void shouldReturnSettingsWithoutBranding() {
            OrganizationConfiguration config = OrganizationConfiguration.builder()
                    .organizationId(ORG_ID)
                    .maxUnitsPerDistribution(100)
                    .enableOwnerPermissionManagement(false)
                    .build();

            when(organizationSettingsUseCase.getSettings(ORG_ID)).thenReturn(Mono.just(config));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/organization/settings"))
                    .build();

            StepVerifier.create(handler.getSettings(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("PUT /api/organization/settings")
    class UpdateSettingsTests {

        @Test
        @DisplayName("Should update settings with branding data successfully")
        void shouldUpdateWithBrandingSuccessfully() {
            OrganizationConfiguration savedConfig = OrganizationConfiguration.builder()
                    .organizationId(ORG_ID)
                    .maxUnitsPerDistribution(50)
                    .enableOwnerPermissionManagement(true)
                    .build();
            OrganizationConfiguration brandedConfig = savedConfig.toBuilder()
                    .dominantColor("#FF8C61")
                    .secondaryColor("#4A90D9")
                    .accentColor("#27AE60")
                    .build();

            when(organizationSettingsUseCase.updateSettings(eq(ORG_ID), any(OrganizationConfiguration.class)))
                    .thenReturn(Mono.just(savedConfig));
            when(organizationSettingsUseCase.updateBranding(eq(ORG_ID), any(), any(), any(), any(), any()))
                    .thenReturn(Mono.just(brandedConfig));

            Map<String, Object> body = Map.of(
                    "maxUnitsPerDistribution", 50,
                    "enableOwnerPermissionManagement", true,
                    "dominantColor", "#FF8C61",
                    "secondaryColor", "#4A90D9",
                    "accentColor", "#27AE60"
            );

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/organization/settings"))
                    .body(Mono.just(body));

            StepVerifier.create(handler.updateSettings(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return 400 when maxUnitsPerDistribution is invalid")
        void shouldReturnBadRequestForInvalidMaxUnits() {
            Map<String, Object> body = Map.of(
                    "maxUnitsPerDistribution", 0
            );

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/organization/settings"))
                    .body(Mono.just(body));

            StepVerifier.create(handler.updateSettings(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return 400 when branding validation fails")
        void shouldReturnBadRequestForInvalidBranding() {
            OrganizationConfiguration savedConfig = OrganizationConfiguration.builder()
                    .organizationId(ORG_ID)
                    .maxUnitsPerDistribution(50)
                    .build();

            when(organizationSettingsUseCase.updateSettings(eq(ORG_ID), any(OrganizationConfiguration.class)))
                    .thenReturn(Mono.just(savedConfig));
            when(organizationSettingsUseCase.updateBranding(eq(ORG_ID), any(), any(), any(), any(), any()))
                    .thenReturn(Mono.error(new BusinessException(
                            "Formato de imagen no soportado. Solo se permiten PNG y JPEG",
                            "INVALID_IMAGE_FORMAT")));

            Map<String, Object> body = Map.of(
                    "maxUnitsPerDistribution", 50,
                    "dominantColor", "#FF8C61"
            );

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/organization/settings"))
                    .body(Mono.just(body));

            StepVerifier.create(handler.updateSettings(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    })
                    .verifyComplete();
        }
    }
}
