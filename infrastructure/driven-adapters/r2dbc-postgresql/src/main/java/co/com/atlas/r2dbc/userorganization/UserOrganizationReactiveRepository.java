package co.com.atlas.r2dbc.userorganization;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para UserOrganization.
 */
public interface UserOrganizationReactiveRepository extends ReactiveCrudRepository<UserOrganizationEntity, Long> {
    
    Flux<UserOrganizationEntity> findByUserId(Long userId);
    
    Flux<UserOrganizationEntity> findByOrganizationId(Long organizationId);
    
    Flux<UserOrganizationEntity> findByUserIdAndStatus(Long userId, String status);
    
    Mono<UserOrganizationEntity> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    Mono<Boolean> existsByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    Mono<Long> countByOrganizationIdAndStatus(Long organizationId, String status);
}
