package co.com.atlas.r2dbc.userrolemulti;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para UserRoleMulti.
 */
public interface UserRoleMultiReactiveRepository extends ReactiveCrudRepository<UserRoleMultiEntity, Long> {
    
    Flux<UserRoleMultiEntity> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    Flux<UserRoleMultiEntity> findByUserId(Long userId);
    
    Flux<UserRoleMultiEntity> findByUserIdAndOrganizationIdIsNull(Long userId);
    
    Mono<Boolean> existsByUserIdAndOrganizationIdAndRoleId(Long userId, Long organizationId, Long roleId);
    
    Mono<Void> deleteByUserIdAndOrganizationId(Long userId, Long organizationId);
}
