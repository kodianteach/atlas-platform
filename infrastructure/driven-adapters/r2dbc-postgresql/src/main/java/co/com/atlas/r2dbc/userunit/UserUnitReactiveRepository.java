package co.com.atlas.r2dbc.userunit;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para UserUnit.
 */
public interface UserUnitReactiveRepository extends ReactiveCrudRepository<UserUnitEntity, Long> {
    
    Flux<UserUnitEntity> findByUserId(Long userId);
    
    Flux<UserUnitEntity> findByUnitId(Long unitId);
    
    @Query("SELECT * FROM user_units WHERE user_id = :userId AND status = 'ACTIVE'")
    Flux<UserUnitEntity> findByUserIdAndStatusActive(Long userId);
    
    Mono<UserUnitEntity> findByUserIdAndUnitId(Long userId, Long unitId);
    
    Mono<UserUnitEntity> findByUserIdAndIsPrimaryTrue(Long userId);
    
    Mono<Boolean> existsByUserIdAndUnitId(Long userId, Long unitId);
    
    @Query("SELECT COUNT(*) FROM user_units WHERE unit_id = :unitId AND status = 'ACTIVE'")
    Mono<Long> countByUnitIdAndStatusActive(Long unitId);
}
