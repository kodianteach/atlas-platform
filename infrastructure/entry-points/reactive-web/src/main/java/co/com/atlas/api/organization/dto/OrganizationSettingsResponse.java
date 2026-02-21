package co.com.atlas.api.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response de configuración de organización.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de configuración de organización")
public class OrganizationSettingsResponse {

    @Schema(description = "Límite máximo de unidades por solicitud de distribución", example = "100")
    private Integer maxUnitsPerDistribution;

    @Schema(description = "Habilitar administración granular de permisos por propietario", example = "false")
    private Boolean enableOwnerPermissionManagement;

    @Schema(description = "Mensaje descriptivo", example = "Configuración actualizada exitosamente")
    private String message;
}
