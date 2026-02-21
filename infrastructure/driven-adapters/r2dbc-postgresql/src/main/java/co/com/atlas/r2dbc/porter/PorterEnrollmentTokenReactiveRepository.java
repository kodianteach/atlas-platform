package co.com.atlas.r2dbc.porter;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para porter_enrollment_tokens.
 */
public interface PorterEnrollmentTokenReactiveRepository extends ReactiveCrudRepository<PorterEnrollmentTokenEntity, Long> {

    Mono<PorterEnrollmentTokenEntity> findByUserIdAndStatus(Long userId, String status);

    Mono<PorterEnrollmentTokenEntity> findByTokenHash(String tokenHash);
}
