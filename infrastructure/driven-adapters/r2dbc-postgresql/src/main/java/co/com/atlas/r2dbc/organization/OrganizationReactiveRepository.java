package co.com.atlas.r2dbc.organization;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para Organization.
 */
public interface OrganizationReactiveRepository extends ReactiveCrudRepository<OrganizationEntity, Long> {
    
    Mono<OrganizationEntity> findByCodeAndDeletedAtIsNull(String code);
    
    Mono<OrganizationEntity> findBySlugAndDeletedAtIsNull(String slug);
    
    Flux<OrganizationEntity> findByCompanyIdAndDeletedAtIsNull(Long companyId);
    
    Flux<OrganizationEntity> findByIsActiveTrueAndDeletedAtIsNull();
    
    Mono<Boolean> existsByCodeAndDeletedAtIsNull(String code);
}
