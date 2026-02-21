package co.com.atlas.api.authorization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para creación de autorización de visitante (parte JSON del multipart).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para creación de autorización de ingreso")
public class AuthorizationRequestDto {

    @Schema(description = "Nombre completo del autorizado", example = "Juan Pérez García", required = true)
    private String personName;

    @Schema(description = "Documento de identidad del autorizado", example = "1234567890", required = true)
    private String personDocument;

    @Schema(description = "Tipo de servicio: DELIVERY, VISIT, TECHNICIAN, OTHER", example = "VISIT", required = true)
    private String serviceType;

    @Schema(description = "Fecha/hora de inicio de vigencia (ISO 8601)", example = "2026-02-20T08:00:00Z", required = true)
    private String validFrom;

    @Schema(description = "Fecha/hora de fin de vigencia (ISO 8601)", example = "2026-02-22T18:00:00Z", required = true)
    private String validTo;

    @Schema(description = "ID de la unidad", example = "1", required = true)
    private Long unitId;

    @Schema(description = "Placa del vehículo (opcional)", example = "ABC123")
    private String vehiclePlate;

    @Schema(description = "Tipo de vehículo: CAR, MOTORCYCLE, OTHER (opcional)", example = "CAR")
    private String vehicleType;

    @Schema(description = "Color del vehículo (opcional)", example = "Rojo")
    private String vehicleColor;
}
