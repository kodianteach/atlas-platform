package co.com.atlas.r2dbc.userunitpermission;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para UserUnitPermission.
 */
public interface UserUnitPermissionReactiveRepository extends ReactiveCrudRepository<UserUnitPermissionEntity, Long> {

    Flux<UserUnitPermissionEntity> findByUserUnitId(Long userUnitId);

    Mono<Void> deleteByUserUnitId(Long userUnitId);
}
