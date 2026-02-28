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

    // Branding fields (HU #10)

    @Schema(description = "Logo de la organización en Base64")
    private String logoBase64;

    @Schema(description = "Tipo MIME del logo", example = "image/png")
    private String logoContentType;

    @Schema(description = "Color dominante en formato hex #RRGGBB", example = "#FF8C61")
    private String dominantColor;

    @Schema(description = "Color secundario en formato hex #RRGGBB", example = "#4A90D9")
    private String secondaryColor;

    @Schema(description = "Color de acento en formato hex #RRGGBB", example = "#27AE60")
    private String accentColor;

    @Schema(description = "Mensaje descriptivo", example = "Configuración actualizada exitosamente")
    private String message;
}
