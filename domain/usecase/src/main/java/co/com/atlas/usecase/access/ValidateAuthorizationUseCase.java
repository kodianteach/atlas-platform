package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Caso de uso para validación online de autorizaciones QR en portería.
 * Verifica firma Ed25519 server-side, valida rango de fechas, verifica
 * estado ACTIVE y registra el evento de acceso.
 */
@RequiredArgsConstructor
public class ValidateAuthorizationUseCase {

    private static final long MAX_CLOCK_SKEW_MINUTES = 10;

    private final CryptoKeyRepository cryptoKeyRepository;
    private final VisitorAuthorizationRepository visitorAuthorizationRepository;
    private final AccessEventRepository accessEventRepository;

    /**
     * Valida una autorización online mediante QR firmado.
     *
     * @param signedQr       QR firmado en formato payload.signature (base64url)
     * @param porterUserId   ID del portero que escanea
     * @param deviceId       ID del dispositivo de portería
     * @param organizationId ID de la organización
     * @return Evento de acceso registrado
     */
    public Mono<AccessEvent> execute(String signedQr, Long porterUserId, String deviceId, Long organizationId) {
        return Mono.fromCallable(() -> parseSignedQr(signedQr))
                .flatMap(parts -> {
                    String payloadBase64 = parts[0];
                    String signatureBase64 = parts[1];
                    String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);

                    Long authId = extractAuthId(payloadJson);
                    String personName = extractField(payloadJson, "personName");
                    String personDocument = extractField(payloadJson, "personDoc");
                    String validFrom = extractField(payloadJson, "validFrom");
                    String validTo = extractField(payloadJson, "validTo");
                    String vehiclePlate = extractField(payloadJson, "vehiclePlate");

                    return cryptoKeyRepository.findActiveByOrganizationId(organizationId)
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    "No se encontró clave criptográfica para la organización", "CRYPTO_KEY_NOT_FOUND")))
                            .flatMap(cryptoKey -> verifySignature(cryptoKey, payloadBase64, signatureBase64)
                                    .flatMap(validSignature -> {
                                        if (!validSignature) {
                                            return createAndSaveEvent(organizationId, authId, porterUserId, deviceId,
                                                    ScanResult.INVALID, personName, personDocument, vehiclePlate,
                                                    "Firma digital inválida");
                                        }

                                        if (!isDateInRange(validFrom, validTo)) {
                                            return createAndSaveEvent(organizationId, authId, porterUserId, deviceId,
                                                    ScanResult.EXPIRED, personName, personDocument, vehiclePlate,
                                                    "Autorización fuera de rango de fechas");
                                        }

                                        return visitorAuthorizationRepository.findById(authId)
                                                .switchIfEmpty(Mono.error(new BusinessException(
                                                        "Autorización no encontrada", "AUTHORIZATION_NOT_FOUND")))
                                                .flatMap(auth -> {
                                                    if (auth.getStatus() != AuthorizationStatus.ACTIVE) {
                                                        return createAndSaveEvent(organizationId, authId, porterUserId,
                                                                deviceId, ScanResult.REVOKED, personName,
                                                                personDocument, vehiclePlate,
                                                                "Autorización revocada o inactiva");
                                                    }

                                                    return createAndSaveEvent(organizationId, authId, porterUserId,
                                                            deviceId, ScanResult.VALID, personName,
                                                            personDocument, vehiclePlate, null);
                                                });
                                    }));
                });
    }

    private String[] parseSignedQr(String signedQr) {
        if (signedQr == null || !signedQr.contains(".")) {
            throw new BusinessException("Formato de QR inválido", "QR_FORMAT_INVALID");
        }
        String[] parts = signedQr.split("\\.", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new BusinessException("Formato de QR inválido", "QR_FORMAT_INVALID");
        }
        return parts;
    }

    private Mono<Boolean> verifySignature(OrganizationCryptoKey cryptoKey, String payloadBase64, String signatureBase64) {
        return Mono.fromCallable(() -> {
            byte[] publicKeyBytes = extractPublicKeyBytes(cryptoKey.getPublicKeyJwk());
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payloadBase64.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);
        }).onErrorReturn(false);
    }

    private byte[] extractPublicKeyBytes(String publicKeyJwk) {
        String xValue = extractField(publicKeyJwk, "x");
        if (xValue == null) {
            throw new BusinessException("Clave pública inválida", "INVALID_PUBLIC_KEY");
        }
        return Base64.getUrlDecoder().decode(xValue);
    }

    private boolean isDateInRange(String validFrom, String validTo) {
        if (validFrom == null || validTo == null) {
            return false;
        }
        Instant now = Instant.now();
        Instant from = Instant.parse(validFrom).minus(MAX_CLOCK_SKEW_MINUTES, ChronoUnit.MINUTES);
        Instant to = Instant.parse(validTo).plus(MAX_CLOCK_SKEW_MINUTES, ChronoUnit.MINUTES);
        return !now.isBefore(from) && !now.isAfter(to);
    }

    private Mono<AccessEvent> createAndSaveEvent(Long orgId, Long authId, Long porterUserId,
                                                  String deviceId, ScanResult result,
                                                  String personName, String personDocument,
                                                  String vehiclePlate, String notes) {
        AccessEvent event = AccessEvent.builder()
                .organizationId(orgId)
                .authorizationId(authId)
                .porterUserId(porterUserId)
                .deviceId(deviceId)
                .action(AccessAction.ENTRY)
                .scanResult(result)
                .personName(personName)
                .personDocument(personDocument)
                .vehiclePlate(vehiclePlate)
                .offlineValidated(false)
                .notes(notes)
                .scannedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        return accessEventRepository.save(event);
    }

    private Long extractAuthId(String json) {
        String value = extractField(json, "authId");
        if (value == null) {
            throw new BusinessException("QR no contiene authId", "QR_MISSING_AUTH_ID");
        }
        return Long.parseLong(value);
    }

    private String extractField(String json, String fieldName) {
        String search = "\"" + fieldName + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) {
            return null;
        }
        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx < 0) {
            return null;
        }
        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }
        if (start >= json.length()) {
            return null;
        }
        // Check if it's a number (no quotes)
        char prevChar = json.charAt(start - 1);
        if (prevChar != '"') {
            // Numeric value
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ' ') {
                end++;
            }
            return json.substring(start, end).trim();
        }
        // String value (already past opening quote)
        int end = json.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return json.substring(start, end);
    }
}
