package co.com.atlas.model.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Value object para el payload del QR firmado.
 * Contiene los datos mínimos necesarios para verificación offline por el portero.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class QrPayload {
    /** ID de la autorización */
    private Long authId;
    /** ID de la organización */
    private Long orgId;
    /** Código de la unidad (e.g. "APT-101") */
    private String unitCode;
    /** Nombre de la persona autorizada */
    private String personName;
    /** Documento de identidad */
    private String personDoc;
    /** Tipo de servicio */
    private String serviceType;
    /** Inicio de vigencia */
    private Instant validFrom;
    /** Fin de vigencia */
    private Instant validTo;
    /** Placa del vehículo (opcional) */
    private String vehiclePlate;
    /** Tipo de vehículo (opcional) */
    private String vehicleType;
    /** Color del vehículo (opcional) */
    private String vehicleColor;
    /** Timestamp de emisión */
    private Instant issuedAt;
    /** Key ID de la clave pública para verificación */
    private String kid;
}
