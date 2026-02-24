package co.com.atlas.model.porter;

/**
 * Value object con el resultado del enrolamiento de un dispositivo de portería.
 * Contiene la clave pública JWK, las reglas offline, el JWT para sesión automática
 * y las credenciales generadas para futuros inicios de sesión.
 */
public record EnrollmentResult(
        Long porterId,
        String porterDisplayName,
        String organizationName,
        String verificationKeyJwk,
        String keyId,
        Integer maxClockSkewMinutes,
        String accessToken,
        String refreshToken,
        String defaultRoute,
        String porterUsername,
        String porterPassword
) {}
