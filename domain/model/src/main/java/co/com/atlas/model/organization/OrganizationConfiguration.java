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

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Retorna el límite máximo con fallback al valor por defecto.
     *
     * @return límite máximo de unidades, nunca null
     */
    public int getMaxUnitsPerDistributionOrDefault() {
        return maxUnitsPerDistribution != null ? maxUnitsPerDistribution : DEFAULT_MAX_UNITS_PER_DISTRIBUTION;
    }
}
