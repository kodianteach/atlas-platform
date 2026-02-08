package co.com.atlas.model.zone.gateways;

import co.com.atlas.model.zone.Zone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Zone.
 */
public interface ZoneRepository {
    
    /**
     * Busca una zona por ID.
     */
    Mono<Zone> findById(Long id);
    
    /**
     * Busca una zona por código dentro de una organización.
     */
    Mono<Zone> findByOrganizationIdAndCode(Long organizationId, String code);
    
    /**
     * Lista todas las zonas de una organización.
     */
    Flux<Zone> findByOrganizationId(Long organizationId);
    
    /**
     * Lista las zonas activas de una organización.
     */
    Flux<Zone> findActiveByOrganizationId(Long organizationId);
    
    /**
     * Guarda o actualiza una zona.
     */
    Mono<Zone> save(Zone zone);
    
    /**
     * Soft delete de una zona.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si existe una zona con el código en la organización.
     */
    Mono<Boolean> existsByOrganizationIdAndCode(Long organizationId, String code);
}
