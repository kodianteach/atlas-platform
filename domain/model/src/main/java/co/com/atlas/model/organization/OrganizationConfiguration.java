package co.com.atlas.model.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para la configuración de una organización.
 * Cada organización tiene una configuración asociada que define límites y parámetros operativos.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrganizationConfiguration {

    /**
     * Valor por defecto del límite máximo de unidades por distribución.
     */
    public static final int DEFAULT_MAX_UNITS_PER_DISTRIBUTION = 100;

    private Long id;
    private Long organizationId;

    /**
     * Límite máximo de unidades que se pueden crear en una sola solicitud de distribución.
     */
    private Integer maxUnitsPerDistribution;

    /**
     * When true, owners can select specific permissions for residents they invite.
     * When false, residents inherit all owner permissions.
     */
    private Boolean enableOwnerPermissionManagement;

    // ── Branding fields (HU #10) ──────────────────────────────────────────────

    /**
     * Logo image bytes (PNG or JPEG). Null when no logo configured.
     */
    private byte[] logoData;

    /**
     * MIME type of the logo image (image/png or image/jpeg).
     */
    private String logoContentType;

    /**
     * Dominant/primary brand color in hex format (#RRGGBB).
     */
    private String dominantColor;

    /**
     * Secondary brand color in hex format (#RRGGBB).
     */
    private String secondaryColor;

    /**
     * Accent brand color in hex format (#RRGGBB).
     */
    private String accentColor;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Validates whether a color string is a valid hex color (#RRGGBB).
     *
     * @param color the color string to validate
     * @return true if the color follows the #RRGGBB pattern
     */
    public static boolean isValidHexColor(String color) {
        if (color == null) {
            return true; // null is valid (means "not configured")
        }
        return color.matches("^#[0-9A-Fa-f]{6}$");
    }

    /**
     * Checks whether this configuration has branding colors defined.
     *
     * @return true if at least the dominant color is configured
     */
    public boolean hasBranding() {
        return dominantColor != null;
    }

    /**
     * Retorna el límite máximo con fallback al valor por defecto.
     *
     * @return límite máximo de unidades, nunca null
     */
    public int getMaxUnitsPerDistributionOrDefault() {
        return maxUnitsPerDistribution != null ? maxUnitsPerDistribution : DEFAULT_MAX_UNITS_PER_DISTRIBUTION;
    }
}
