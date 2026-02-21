package co.com.atlas.model.porter;

/**
 * Value object con el resultado del enrolamiento de un dispositivo de portería.
 * Contiene la clave pública JWK y las reglas offline para el dispositivo.
 */
public record EnrollmentResult(
        Long porterId,
        String porterDisplayName,
        String organizationName,
        String verificationKeyJwk,
        String keyId,
        Integer maxClockSkewMinutes
) {}
