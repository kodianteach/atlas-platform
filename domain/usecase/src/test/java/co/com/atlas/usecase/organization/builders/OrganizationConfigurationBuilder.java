package co.com.atlas.usecase.organization.builders;

import co.com.atlas.model.organization.OrganizationConfiguration;

import java.time.Instant;

/**
 * Test Data Builder for OrganizationConfiguration.
 * Follows fluent builder pattern consistent with PorterBuilder.
 */
public class OrganizationConfigurationBuilder {

    private Long id = 1L;
    private Long organizationId = 100L;
    private Integer maxUnitsPerDistribution = 100;
    private Boolean enableOwnerPermissionManagement = false;
    private byte[] logoData = null;
    private String logoContentType = null;
    private String dominantColor = null;
    private String secondaryColor = null;
    private String accentColor = null;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = null;

    public static OrganizationConfigurationBuilder anOrganizationConfig() {
        return new OrganizationConfigurationBuilder();
    }

    public OrganizationConfigurationBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public OrganizationConfigurationBuilder withOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public OrganizationConfigurationBuilder withMaxUnitsPerDistribution(Integer maxUnitsPerDistribution) {
        this.maxUnitsPerDistribution = maxUnitsPerDistribution;
        return this;
    }

    public OrganizationConfigurationBuilder withEnableOwnerPermissionManagement(Boolean enabled) {
        this.enableOwnerPermissionManagement = enabled;
        return this;
    }

    public OrganizationConfigurationBuilder withLogoData(byte[] logoData) {
        this.logoData = logoData;
        return this;
    }

    public OrganizationConfigurationBuilder withLogoContentType(String logoContentType) {
        this.logoContentType = logoContentType;
        return this;
    }

    public OrganizationConfigurationBuilder withDominantColor(String dominantColor) {
        this.dominantColor = dominantColor;
        return this;
    }

    public OrganizationConfigurationBuilder withSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
        return this;
    }

    public OrganizationConfigurationBuilder withAccentColor(String accentColor) {
        this.accentColor = accentColor;
        return this;
    }

    public OrganizationConfigurationBuilder withBranding(String dominantColor, String secondaryColor, String accentColor) {
        this.dominantColor = dominantColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
        return this;
    }

    public OrganizationConfiguration build() {
        return OrganizationConfiguration.builder()
                .id(id)
                .organizationId(organizationId)
                .maxUnitsPerDistribution(maxUnitsPerDistribution)
                .enableOwnerPermissionManagement(enableOwnerPermissionManagement)
                .logoData(logoData)
                .logoContentType(logoContentType)
                .dominantColor(dominantColor)
                .secondaryColor(secondaryColor)
                .accentColor(accentColor)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
