package co.com.atlas.r2dbc.userunit;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para UserUnit.
 */
public interface UserUnitReactiveRepository extends ReactiveCrudRepository<UserUnitEntity, Long> {
    
    Flux<UserUnitEntity> findByUserId(Long userId);
    
    Flux<UserUnitEntity> findByUnitId(Long unitId);
    
    Flux<UserUnitEntity> findByUserIdAndIsActiveTrue(Long userId);
    
    Mono<UserUnitEntity> findByUserIdAndUnitId(Long userId, Long unitId);
    
    Mono<UserUnitEntity> findByUserIdAndIsPrimaryTrue(Long userId);
    
    Mono<Boolean> existsByUserIdAndUnitId(Long userId, Long unitId);
    
    Mono<Long> countByUnitIdAndIsActiveTrue(Long unitId);
}
