package co.com.atlas.model.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO para configuración de organización.
 * Representa los parámetros configurables almacenados en el campo JSON 'settings' de Organization.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class OrganizationSettings {

    /**
     * Valor por defecto del límite máximo de unidades por distribución.
     */
    public static final int DEFAULT_MAX_UNITS_PER_DISTRIBUTION = 100;

    /**
     * Límite máximo de unidades que se pueden crear en una sola solicitud de distribución.
     */
    @Builder.Default
    private Integer maxUnitsPerDistribution = DEFAULT_MAX_UNITS_PER_DISTRIBUTION;

    /**
     * When true, owners can select specific permissions for residents they invite.
     * When false, residents inherit all owner permissions.
     */
    @Builder.Default
    private Boolean enableOwnerPermissionManagement = false;

    /**
     * Retorna el límite máximo con fallback al valor por defecto.
     *
     * @return límite máximo de unidades, nunca null
     */
    public int getMaxUnitsPerDistributionOrDefault() {
        return maxUnitsPerDistribution != null ? maxUnitsPerDistribution : DEFAULT_MAX_UNITS_PER_DISTRIBUTION;
    }
}
