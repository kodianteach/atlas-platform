package co.com.atlas.model.userunit.gateways;

import co.com.atlas.model.userunit.UserUnit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de UserUnit.
 */
public interface UserUnitRepository {
    
    /**
     * Busca una vinculación por ID.
     */
    Mono<UserUnit> findById(Long id);
    
    /**
     * Busca la vinculación de un usuario en una unidad.
     */
    Mono<UserUnit> findByUserIdAndUnitId(Long userId, Long unitId);
    
    /**
     * Lista las unidades de un usuario.
     */
    Flux<UserUnit> findByUserId(Long userId);
    
    /**
     * Lista los usuarios de una unidad.
     */
    Flux<UserUnit> findByUnitId(Long unitId);
    
    /**
     * Lista las unidades activas de un usuario.
     */
    Flux<UserUnit> findActiveByUserId(Long userId);
    
    /**
     * Busca la unidad primaria de un usuario.
     */
    Mono<UserUnit> findPrimaryByUserId(Long userId);
    
    /**
     * Guarda o actualiza una vinculación.
     */
    Mono<UserUnit> save(UserUnit userUnit);
    
    /**
     * Elimina una vinculación.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si un usuario está vinculado a una unidad.
     */
    Mono<Boolean> existsByUserIdAndUnitId(Long userId, Long unitId);
    
    /**
     * Cuenta las vinculaciones activas de una unidad.
     */
    Mono<Long> countActiveByUnit(Long unitId);
}
