package co.com.atlas.model.tower.gateways;

import co.com.atlas.model.tower.Tower;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Tower.
 */
public interface TowerRepository {
    
    /**
     * Busca una torre por ID.
     */
    Mono<Tower> findById(Long id);
    
    /**
     * Busca una torre por código dentro de una zona.
     */
    Mono<Tower> findByZoneIdAndCode(Long zoneId, String code);
    
    /**
     * Lista todas las torres de una zona.
     */
    Flux<Tower> findByZoneId(Long zoneId);
    
    /**
     * Lista las torres activas de una zona.
     */
    Flux<Tower> findActiveByZoneId(Long zoneId);
    
    /**
     * Lista las torres de una organización.
     */
    Flux<Tower> findByOrganizationId(Long organizationId);
    
    /**
     * Guarda o actualiza una torre.
     */
    Mono<Tower> save(Tower tower);
    
    /**
     * Soft delete de una torre.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si existe una torre con el código en la zona.
     */
    Mono<Boolean> existsByZoneIdAndCode(Long zoneId, String code);
}
