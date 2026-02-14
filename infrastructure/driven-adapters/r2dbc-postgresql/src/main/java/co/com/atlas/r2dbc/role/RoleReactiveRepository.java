package co.com.atlas.r2dbc.role;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Role.
 */
public interface RoleReactiveRepository extends ReactiveCrudRepository<RoleEntity, Long> {
    
    Mono<RoleEntity> findByCode(String code);
}
