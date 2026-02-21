package co.com.atlas.usecase.porter;

import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.porter.PorterEnrollmentAuditAction;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.porter.gateways.PorterRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Caso de uso para regenerar URL de enrolamiento de portero.
 * Un portero es un usuario con rol porter; su ID = users.id.
 */
@RequiredArgsConstructor
public class RegeneratePorterEnrollmentUrlUseCase {

    private final PorterRepository porterRepository;
    private final PorterEnrollmentTokenRepository tokenRepository;
    private final PorterEnrollmentAuditRepository auditRepository;

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final String ENROLLMENT_BASE_URL = "/porter-enroll";

    /**
     * Resultado de la regeneración.
     */
    public record RegenerateResult(String enrollmentUrl) {}

    /**
     * Regenera la URL de enrolamiento para un portero existente.
     *
     * @param porterId       ID del usuario portero (users.id)
     * @param organizationId ID de la organización
     * @param adminUserId    ID del admin que regenera la URL
     * @return Nueva URL de enrolamiento
     */
    public Mono<RegenerateResult> execute(Long porterId, Long organizationId, Long adminUserId) {

        return porterRepository.findByUserIdAndOrganizationId(porterId, organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Porter", porterId)))
                .flatMap(porter -> revokeActiveToken(porter.getId())
                        .then(generateNewToken(porter.getId(), organizationId, adminUserId))
                        .flatMap(tokenAndUrl -> auditRepository.save(PorterEnrollmentAuditLog.builder()
                                        .tokenId(tokenAndUrl.token.getId())
                                        .action(PorterEnrollmentAuditAction.URL_REGENERATED)
                                        .performedBy(adminUserId)
                                        .build())
                                .thenReturn(new RegenerateResult(tokenAndUrl.url))
                        )
                );
    }

    private Mono<Void> revokeActiveToken(Long userId) {
        return tokenRepository.findActiveByUserId(userId)
                .flatMap(activeToken -> {
                    PorterEnrollmentToken revoked = activeToken.toBuilder()
                            .status(PorterEnrollmentTokenStatus.REVOKED)
                            .build();
                    return tokenRepository.save(revoked);
                })
                .then();
    }

    private record TokenAndUrl(PorterEnrollmentToken token, String url) {}

    private Mono<TokenAndUrl> generateNewToken(Long userId, Long organizationId, Long adminUserId) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS);

        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .userId(userId)
                .organizationId(organizationId)
                .tokenHash(tokenHash)
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(expiresAt)
                .createdBy(adminUserId)
                .build();

        return tokenRepository.save(token)
                .map(saved -> {
                    String url = ENROLLMENT_BASE_URL + "?token=" + rawToken;
                    return new TokenAndUrl(saved, url);
                });
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
