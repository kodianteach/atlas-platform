package co.com.atlas.api.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar configuración de organización.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para actualizar configuración de organización")
public class OrganizationSettingsDto {

    @Schema(description = "Límite máximo de unidades por solicitud de distribución", 
            example = "100", required = true)
    private Integer maxUnitsPerDistribution;

    @Schema(description = "Habilitar administración granular de permisos por propietario",
            example = "false")
    private Boolean enableOwnerPermissionManagement;
}
