package co.com.atlas.api.external.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de request para enrolamiento de dispositivo de portería.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de enrolamiento de dispositivo de portería")
public class EnrollDeviceRequest {

    @Schema(description = "Token de enrolamiento recibido del enlace", required = true)
    private String token;

    @Schema(description = "Plataforma del dispositivo", example = "Android")
    private String platform;

    @Schema(description = "Modelo del dispositivo", example = "Samsung SM-T510")
    private String model;

    @Schema(description = "Versión de la aplicación", example = "1.0.0")
    private String appVersion;
}
