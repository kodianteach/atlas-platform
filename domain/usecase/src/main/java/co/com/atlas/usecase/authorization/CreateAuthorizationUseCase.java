package co.com.atlas.usecase.authorization;

import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.QrPayload;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.FileStorageGateway;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyGeneratorGateway;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Caso de uso para creación de autorizaciones de ingreso con QR firmado.
 * Flujo: Valida permisos → Almacena PDF → Construye payload → Firma Ed25519 → Persiste.
 */
@RequiredArgsConstructor
public class CreateAuthorizationUseCase {

    private final VisitorAuthorizationRepository authorizationRepository;
    private final FileStorageGateway fileStorageGateway;
    private final CryptoKeyRepository cryptoKeyRepository;
    private final CryptoKeyGeneratorGateway cryptoKeyGeneratorGateway;
    private final UserUnitRepository userUnitRepository;
    private final UnitRepository unitRepository;

    /**
     * Crea una nueva autorización de ingreso con QR firmado digitalmente.
     *
     * @param authorization Datos de la autorización (sin signedQr ni id)
     * @param pdfContent    Contenido del documento de identidad en PDF
     * @param userId        ID del usuario que crea la autorización
     * @return Mono con la autorización creada incluyendo el QR firmado
     */
    public Mono<VisitorAuthorization> execute(VisitorAuthorization authorization,
                                               byte[] pdfContent,
                                               Long userId) {
        return validateDates(authorization)
                .then(resolveUnitId(authorization, userId))
                .flatMap(resolvedAuth -> resolveUnitCode(resolvedAuth.getUnitId())
                        .flatMap(unitCode -> storePdfAndSign(resolvedAuth, pdfContent, userId, unitCode)));
    }

    /**
     * Resuelve el unitId automáticamente cuando no se proporciona.
     * Para TENANT/FAMILY/OWNER usa la unidad primaria del usuario.
     */
    private Mono<VisitorAuthorization> resolveUnitId(VisitorAuthorization authorization, Long userId) {
        if (authorization.getUnitId() != null) {
            return Mono.just(authorization);
        }
        return userUnitRepository.findPrimaryByUserId(userId)
                .switchIfEmpty(
                    Mono.defer(() -> userUnitRepository.findActiveByUserId(userId)
                            .next()
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    "No se encontró una unidad asignada al usuario. Contacte al administrador.",
                                    "USER_UNIT_NOT_FOUND"))))
                )
                .map(userUnit -> {
                    Long resolvedUnitId = userUnit.getUnitId();
                    if (resolvedUnitId == null) {
                        throw new BusinessException(
                                "La unidad asignada al usuario no tiene un ID válido",
                                "INVALID_USER_UNIT");
                    }
                    return authorization.toBuilder()
                            .unitId(resolvedUnitId)
                            .build();
                });
    }

    private Mono<Void> validateDates(VisitorAuthorization authorization) {
        if (authorization.getValidFrom() == null || authorization.getValidTo() == null) {
            return Mono.error(new BusinessException(
                    "Las fechas de inicio y fin son obligatorias", "MISSING_DATES"));
        }
        if (!authorization.getValidTo().isAfter(authorization.getValidFrom())) {
            return Mono.error(new BusinessException(
                    "La fecha de salida debe ser posterior a la fecha de ingreso", "INVALID_DATE_RANGE"));
        }
        if (authorization.getValidFrom().isBefore(Instant.now().minusSeconds(60))) {
            return Mono.error(new BusinessException(
                    "La fecha de inicio no puede ser en el pasado", "PAST_START_DATE"));
        }
        return Mono.empty();
    }

    private Mono<String> resolveUnitCode(Long unitId) {
        if (unitId == null) {
            return Mono.error(new BusinessException(
                    "El ID de unidad es obligatorio para crear una autorización", "UNIT_ID_REQUIRED"));
        }
        return unitRepository.findById(unitId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        "Unidad no encontrada", "UNIT_NOT_FOUND")))
                .map(co.com.atlas.model.unit.Unit::getCode);
    }

    private Mono<VisitorAuthorization> storePdfAndSign(VisitorAuthorization authorization,
                                                        byte[] pdfContent,
                                                        Long userId,
                                                        String unitCode) {
        String fileKey = authorization.getOrganizationId() + "/authorizations/"
                + UUID.randomUUID() + "/identity.pdf";

        return fileStorageGateway.store(fileKey, pdfContent, "application/pdf")
                .flatMap(storedKey -> {
                    VisitorAuthorization toSave = authorization.toBuilder()
                            .createdByUserId(userId)
                            .identityDocumentKey(storedKey)
                            .status(AuthorizationStatus.ACTIVE)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return signAndPersist(toSave, unitCode);
                });
    }

    private Mono<VisitorAuthorization> signAndPersist(VisitorAuthorization authorization, String unitCode) {
        return getOrCreateCryptoKey(authorization.getOrganizationId())
                .flatMap(cryptoKey -> buildSignedQr(authorization, unitCode, cryptoKey))
                .flatMap(authorizationRepository::save);
    }

    /**
     * Obtiene la clave activa de la organización o genera una nueva (lazy).
     */
    private Mono<OrganizationCryptoKey> getOrCreateCryptoKey(Long organizationId) {
        return cryptoKeyRepository.findActiveByOrganizationId(organizationId)
                .switchIfEmpty(Mono.defer(() ->
                        cryptoKeyGeneratorGateway.generateForOrganization(organizationId)
                                .flatMap(cryptoKeyRepository::save)
                ));
    }

    private Mono<VisitorAuthorization> buildSignedQr(VisitorAuthorization authorization,
                                                      String unitCode,
                                                      OrganizationCryptoKey cryptoKey) {
        QrPayload payload = QrPayload.builder()
                .authId(authorization.getId())
                .orgId(authorization.getOrganizationId())
                .unitCode(unitCode)
                .personName(authorization.getPersonName())
                .personDoc(authorization.getPersonDocument())
                .serviceType(authorization.getServiceType() != null
                        ? authorization.getServiceType().name() : null)
                .validFrom(authorization.getValidFrom())
                .validTo(authorization.getValidTo())
                .vehiclePlate(authorization.getVehiclePlate())
                .vehicleType(authorization.getVehicleType())
                .vehicleColor(authorization.getVehicleColor())
                .issuedAt(Instant.now())
                .kid(cryptoKey.getKeyId())
                .build();

        String payloadJson = serializePayload(payload);
        String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        return cryptoKeyGeneratorGateway.signPayload(payloadBase64, cryptoKey.getPrivateKeyEncrypted())
                .map(signatureBase64 -> {
                    String signedQr = payloadBase64 + "." + signatureBase64;
                    return authorization.toBuilder()
                            .signedQr(signedQr)
                            .build();
                });
    }

    private String serializePayload(QrPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"authId\":").append(payload.getAuthId()).append(",");
        sb.append("\"orgId\":").append(payload.getOrgId()).append(",");
        appendString(sb, "unitCode", payload.getUnitCode());
        appendString(sb, "personName", payload.getPersonName());
        appendString(sb, "personDoc", payload.getPersonDoc());
        appendString(sb, "serviceType", payload.getServiceType());
        appendString(sb, "validFrom", payload.getValidFrom() != null ? payload.getValidFrom().toString() : null);
        appendString(sb, "validTo", payload.getValidTo() != null ? payload.getValidTo().toString() : null);
        if (payload.getVehiclePlate() != null) {
            appendString(sb, "vehiclePlate", payload.getVehiclePlate());
            appendString(sb, "vehicleType", payload.getVehicleType());
            appendString(sb, "vehicleColor", payload.getVehicleColor());
        }
        appendString(sb, "issuedAt", payload.getIssuedAt() != null ? payload.getIssuedAt().toString() : null);
        appendString(sb, "kid", payload.getKid());
        // Remove trailing comma
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    private void appendString(StringBuilder sb, String key, String value) {
        if (value != null) {
            sb.append("\"").append(key).append("\":\"").append(value).append("\",");
        }
    }
}
