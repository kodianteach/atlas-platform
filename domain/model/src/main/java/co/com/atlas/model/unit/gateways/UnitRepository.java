package co.com.atlas.model.unit.gateways;

import co.com.atlas.model.unit.Unit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Unit.
 */
public interface UnitRepository {
    
    /**
     * Busca una unidad por ID.
     */
    Mono<Unit> findById(Long id);
    
    /**
     * Busca una unidad por código dentro de una organización.
     */
    Mono<Unit> findByOrganizationIdAndCode(Long organizationId, String code);
    
    /**
     * Lista todas las unidades de una organización.
     */
    Flux<Unit> findByOrganizationId(Long organizationId);
    
    /**
     * Lista las unidades de una zona.
     */
    Flux<Unit> findByZoneId(Long zoneId);
    
    /**
     * Lista las unidades de una torre.
     */
    Flux<Unit> findByTowerId(Long towerId);
    
    /**
     * Lista las unidades de un usuario.
     */
    Flux<Unit> findByUserId(Long userId);
    
    /**
     * Guarda o actualiza una unidad.
     */
    Mono<Unit> save(Unit unit);
    
    /**
     * Soft delete de una unidad.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si existe una unidad con el código en la organización.
     */
    Mono<Boolean> existsByOrganizationIdAndCode(Long organizationId, String code);
}
