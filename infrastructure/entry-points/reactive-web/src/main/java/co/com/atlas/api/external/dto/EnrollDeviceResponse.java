package co.com.atlas.api.external.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta del enrolamiento de dispositivo de portería.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado del enrolamiento de dispositivo de portería")
public class EnrollDeviceResponse {

    @Schema(description = "ID del portero")
    private Long porterId;

    @Schema(description = "Nombre del portero para display", example = "Portería Principal")
    private String porterDisplayName;

    @Schema(description = "Nombre de la organización", example = "Conjunto El Bosque")
    private String organizationName;

    @Schema(description = "Clave pública JWK para verificación offline de QRs")
    private String verificationKeyJwk;

    @Schema(description = "Key Identifier (kid) de la clave", example = "550e8400-e29b-41d4-a716-446655440000")
    private String keyId;

    @Schema(description = "Máximo desfase de reloj permitido en minutos", example = "5")
    private Integer maxClockSkewMinutes;
}
