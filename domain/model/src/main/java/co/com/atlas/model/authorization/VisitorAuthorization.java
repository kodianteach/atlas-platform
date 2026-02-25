package co.com.atlas.model.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para autorización de ingreso con QR firmado.
 * Representa una autorización directa (sin paso de aprobación) que genera
 * un QR firmado digitalmente con la clave privada de la organización.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class VisitorAuthorization {
    private Long id;
    private Long organizationId;
    private Long unitId;
    private Long createdByUserId;
    private String personName;
    private String personDocument;
    private ServiceType serviceType;
    private Instant validFrom;
    private Instant validTo;
    private String vehiclePlate;
    private String vehicleType;
    private String vehicleColor;
    private String identityDocumentKey;
    private String signedQr;
    private AuthorizationStatus status;
    private Instant revokedAt;
    private Long revokedBy;
    private Instant createdAt;
    private Instant updatedAt;

    // Transient enriched fields (not persisted)
    private String unitCode;
    private String createdByUserName;
}
