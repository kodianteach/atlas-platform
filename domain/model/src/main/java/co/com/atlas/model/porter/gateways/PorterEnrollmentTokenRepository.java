package co.com.atlas.model.porter.gateways;

import co.com.atlas.model.porter.PorterEnrollmentToken;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de PorterEnrollmentToken.
 */
public interface PorterEnrollmentTokenRepository {

    /**
     * Guarda o actualiza un token de enrolamiento.
     */
    Mono<PorterEnrollmentToken> save(PorterEnrollmentToken token);

    /**
     * Busca un token activo (PENDING) por el ID del usuario portero.
     */
    Mono<PorterEnrollmentToken> findActiveByUserId(Long userId);

    /**
     * Busca un token por su hash.
     */
    Mono<PorterEnrollmentToken> findByTokenHash(String tokenHash);
}
