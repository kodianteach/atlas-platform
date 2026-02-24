package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.authorization.AuthorizationStatus;
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

                    // Support compact keys (a,n,d,f,t,p) and legacy keys (authId,personName,...)
                    Long authId = extractAuthIdCompat(payloadJson);
                    String personName = extractFieldCompat(payloadJson, "n", "personName");
                    String personDocument = extractFieldCompat(payloadJson, "d", "personDoc");
                    String validFrom = extractDateFieldCompat(payloadJson, "f", "validFrom");
                    String validTo = extractDateFieldCompat(payloadJson, "t", "validTo");
                    String vehiclePlate = extractFieldCompat(payloadJson, "p", "vehiclePlate");

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

    private Long extractAuthIdCompat(String json) {
        // Try compact key "a" first, then legacy "authId"
        String value = extractField(json, "a");
        if (value == null) {
            value = extractField(json, "authId");
        }
        if (value == null) {
            throw new BusinessException("QR no contiene authId", "QR_MISSING_AUTH_ID");
        }
        return Long.parseLong(value);
    }

    private String extractFieldCompat(String json, String compactKey, String legacyKey) {
        String value = extractField(json, compactKey);
        return value != null ? value : extractField(json, legacyKey);
    }

    /**
     * Extracts a date field supporting both compact (epoch seconds number) and legacy (ISO string).
     */
    private String extractDateFieldCompat(String json, String compactKey, String legacyKey) {
        String value = extractField(json, compactKey);
        if (value != null) {
            // Compact format: epoch seconds → convert to ISO string
            long epochSeconds = Long.parseLong(value);
            return Instant.ofEpochSecond(epochSeconds).toString();
        }
        return extractField(json, legacyKey);
    }

    private String extractField(String json, String fieldName) {
        return findFieldValue(json, "\"" + fieldName + "\":");
    }

    private String findFieldValue(String json, String searchKey) {
        int idx = findKeyIndex(json, searchKey);
        if (idx < 0) {
            return null;
        }

        int start = idx + searchKey.length();
        start = skipWhitespace(json, start);
        if (start >= json.length()) {
            return null;
        }

        if (json.charAt(start) == '"') {
            return extractStringValue(json, start + 1);
        }
        if (json.startsWith("null", start)) {
            return null;
        }
        return extractNumericValue(json, start);
    }

    private int findKeyIndex(String json, String searchKey) {
        int idx = json.indexOf(searchKey);
        // Ensure this key is at top-level (preceded by { , or whitespace, not inside a string)
        while (idx > 0 && json.charAt(idx - 1) != '{' && json.charAt(idx - 1) != ','
                && json.charAt(idx - 1) != ' ' && json.charAt(idx - 1) != '\n') {
            idx = json.indexOf(searchKey, idx + searchKey.length());
        }
        return idx;
    }

    private int skipWhitespace(String json, int start) {
        int pos = start;
        while (pos < json.length() && json.charAt(pos) == ' ') {
            pos++;
        }
        return pos;
    }

    private String extractStringValue(String json, int start) {
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private String extractNumericValue(String json, int start) {
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim();
    }
}
