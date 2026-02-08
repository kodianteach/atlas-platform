package co.com.atlas.r2dbc.authuser;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para la entidad AuthUserEntity.
 */
public interface AuthUserReactiveRepository extends ReactiveCrudRepository<AuthUserEntity, Long> {
    
    Mono<AuthUserEntity> findByEmailAndDeletedAtIsNull(String email);
    
    @Query("UPDATE users SET last_login_at = NOW() WHERE id = :userId")
    Mono<Void> updateLastLoginAt(Long userId);
    
    @Query("UPDATE users SET last_organization_id = :organizationId WHERE id = :userId")
    Mono<Void> updateLastOrganizationId(Long userId, Long organizationId);
}
