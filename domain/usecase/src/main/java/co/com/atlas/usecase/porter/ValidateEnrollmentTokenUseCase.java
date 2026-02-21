package co.com.atlas.usecase.porter;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.porter.gateways.PorterRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Caso de uso para validar un token de enrolamiento de portero (sin consumirlo).
 * Verifica que el token exista, esté pendiente y no haya expirado.
 * Retorna información básica para mostrar la pantalla de enrolamiento.
 */
@RequiredArgsConstructor
public class ValidateEnrollmentTokenUseCase {

    private final PorterEnrollmentTokenRepository tokenRepository;
    private final PorterRepository porterRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Resultado de validación del token de enrolamiento.
     */
    public record TokenValidationResult(
            Long porterId,
            String porterName,
            String organizationName,
            Instant expiresAt,
            boolean valid
    ) {}

    /**
     * Valida el token de enrolamiento sin consumirlo.
     *
     * @param rawToken Token en claro recibido del enlace de enrolamiento
     * @return Información básica del portero y organización si el token es válido
     */
    public Mono<TokenValidationResult> execute(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Mono.error(new BusinessException(
                    "Token de enrolamiento requerido", "TOKEN_REQUIRED"));
        }

        String tokenHash = hashToken(rawToken);

        return tokenRepository.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new NotFoundException("Token inválido o no encontrado")))
                .flatMap(this::validateTokenState)
                .flatMap(this::enrichWithDetails);
    }

    private Mono<PorterEnrollmentToken> validateTokenState(PorterEnrollmentToken token) {
        if (token.getStatus() == PorterEnrollmentTokenStatus.CONSUMED) {
            return Mono.error(new BusinessException(
                    "Este token ya fue utilizado", "TOKEN_ALREADY_USED"));
        }
        if (token.getStatus() == PorterEnrollmentTokenStatus.REVOKED) {
            return Mono.error(new BusinessException(
                    "Este token fue revocado", "TOKEN_REVOKED"));
        }
        if (token.getStatus() == PorterEnrollmentTokenStatus.EXPIRED || token.isExpired()) {
            return Mono.error(new BusinessException(
                    "Este token ha expirado", "TOKEN_EXPIRED"));
        }
        if (token.getStatus() != PorterEnrollmentTokenStatus.PENDING) {
            return Mono.error(new BusinessException(
                    "Token en estado inválido", "TOKEN_INVALID_STATE"));
        }
        return Mono.just(token);
    }

    private Mono<TokenValidationResult> enrichWithDetails(PorterEnrollmentToken token) {
        Mono<Porter> porterMono = porterRepository
                .findByUserIdAndOrganizationId(token.getUserId(), token.getOrganizationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Portero no encontrado")));

        Mono<Organization> orgMono = organizationRepository
                .findById(token.getOrganizationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Organización no encontrada")));

        return Mono.zip(porterMono, orgMono)
                .map(tuple -> new TokenValidationResult(
                        tuple.getT1().getId(),
                        tuple.getT1().getNames(),
                        tuple.getT2().getName(),
                        token.getExpiresAt(),
                        true
                ));
    }

    /**
     * Calcula el hash SHA-256 del token en claro.
     * Mismo algoritmo usado en ActivateAdminUseCase.
     */
    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
