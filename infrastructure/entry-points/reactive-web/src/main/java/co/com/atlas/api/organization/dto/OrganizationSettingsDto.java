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

    // Branding fields (HU #10)

    @Schema(description = "Logo de la organización en Base64",
            example = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVQI12NgAAIABQABN...")
    private String logoBase64;

    @Schema(description = "Tipo MIME del logo (image/png o image/jpeg)",
            example = "image/png")
    private String logoContentType;

    @Schema(description = "Color dominante en formato hex #RRGGBB",
            example = "#FF8C61")
    private String dominantColor;

    @Schema(description = "Color secundario en formato hex #RRGGBB",
            example = "#4A90D9")
    private String secondaryColor;

    @Schema(description = "Color de acento en formato hex #RRGGBB",
            example = "#27AE60")
    private String accentColor;
}
