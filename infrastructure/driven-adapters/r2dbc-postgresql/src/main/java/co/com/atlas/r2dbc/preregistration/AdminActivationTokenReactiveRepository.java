package co.com.atlas.r2dbc.preregistration;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para admin_activation_tokens.
 */
public interface AdminActivationTokenReactiveRepository 
        extends ReactiveCrudRepository<AdminActivationTokenEntity, Long> {
    
    /**
     * Busca un token por su hash.
     */
    Mono<AdminActivationTokenEntity> findByTokenHash(String tokenHash);
    
    /**
     * Busca todos los tokens de un usuario.
     */
    Flux<AdminActivationTokenEntity> findByUserId(Long userId);
    
    /**
     * Busca el token pendiente más reciente de un usuario.
     */
    @Query("SELECT * FROM admin_activation_tokens " +
           "WHERE user_id = :userId AND status = 'PENDING' " +
           "ORDER BY created_at DESC LIMIT 1")
    Mono<AdminActivationTokenEntity> findLatestPendingByUserId(Long userId);
    
    /**
     * Busca tokens por estado.
     */
    Flux<AdminActivationTokenEntity> findByStatus(String status);
    
    /**
     * Busca tokens expirados que aún tienen estado PENDING.
     */
    @Query("SELECT * FROM admin_activation_tokens " +
           "WHERE status = 'PENDING' AND expires_at < NOW()")
    Flux<AdminActivationTokenEntity> findExpiredPendingTokens();
    
    /**
     * Verifica si existe un token pendiente válido para el usuario.
     */
    @Query("SELECT COUNT(*) > 0 FROM admin_activation_tokens " +
           "WHERE user_id = :userId AND status = 'PENDING' AND expires_at > NOW()")
    Mono<Boolean> existsValidTokenForUser(Long userId);
}
