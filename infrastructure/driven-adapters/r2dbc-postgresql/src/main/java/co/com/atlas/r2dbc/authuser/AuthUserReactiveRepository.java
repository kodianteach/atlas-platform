package co.com.atlas.r2dbc.authuser;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para la entidad AuthUserEntity.
 */
public interface AuthUserReactiveRepository extends ReactiveCrudRepository<AuthUserEntity, Long> {
    
    Mono<AuthUserEntity> findByEmailAndDeletedAtIsNull(String email);
    
    Mono<AuthUserEntity> findByUsernameAndDeletedAtIsNull(String username);
    
    @Query("UPDATE users SET last_login_at = NOW() WHERE id = :userId")
    Mono<Void> updateLastLoginAt(Long userId);
    
    @Query("UPDATE users SET last_organization_id = :organizationId WHERE id = :userId")
    Mono<Void> updateLastOrganizationId(Long userId, Long organizationId);
    
    /**
     * Verifica si existe un usuario con el tipo y número de documento.
     */
    Mono<Boolean> existsByDocumentTypeAndDocumentNumberAndDeletedAtIsNull(String documentType, String documentNumber);
    
    /**
     * Busca un usuario por tipo y número de documento.
     */
    Mono<AuthUserEntity> findByDocumentTypeAndDocumentNumberAndDeletedAtIsNull(String documentType, String documentNumber);
    
    /**
     * Verifica si existe un usuario con el email.
     */
    Mono<Boolean> existsByEmailAndDeletedAtIsNull(String email);
}
