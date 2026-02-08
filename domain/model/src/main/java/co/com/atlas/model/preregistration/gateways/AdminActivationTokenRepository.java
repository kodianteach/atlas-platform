package co.com.atlas.model.preregistration.gateways;

import co.com.atlas.model.preregistration.AdminActivationToken;
import co.com.atlas.model.preregistration.ActivationTokenStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de AdminActivationToken.
 */
public interface AdminActivationTokenRepository {
    
    /**
     * Busca un token por ID.
     */
    Mono<AdminActivationToken> findById(Long id);
    
    /**
     * Busca un token por su hash.
     */
    Mono<AdminActivationToken> findByTokenHash(String tokenHash);
    
    /**
     * Busca tokens por ID de usuario.
     */
    Flux<AdminActivationToken> findByUserId(Long userId);
    
    /**
     * Busca el token pendiente más reciente de un usuario.
     */
    Mono<AdminActivationToken> findLatestPendingByUserId(Long userId);
    
    /**
     * Lista tokens por estado.
     */
    Flux<AdminActivationToken> findByStatus(ActivationTokenStatus status);
    
    /**
     * Lista tokens expirados que aún tienen estado PENDING.
     */
    Flux<AdminActivationToken> findExpiredPendingTokens();
    
    /**
     * Guarda o actualiza un token.
     */
    Mono<AdminActivationToken> save(AdminActivationToken token);
    
    /**
     * Verifica si existe un token pendiente no expirado para el usuario.
     */
    Mono<Boolean> existsValidTokenForUser(Long userId);
}
