package co.com.atlas.model.visit.gateways;

import co.com.atlas.model.visit.VisitRequest;
import co.com.atlas.model.visit.VisitStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de VisitRequest.
 */
public interface VisitRequestRepository {
    
    /**
     * Busca una solicitud por ID.
     */
    Mono<VisitRequest> findById(Long id);
    
    /**
     * Lista las solicitudes de una organización.
     */
    Flux<VisitRequest> findByOrganizationId(Long organizationId);
    
    /**
     * Lista las solicitudes de una unidad.
     */
    Flux<VisitRequest> findByUnitId(Long unitId);
    
    /**
     * Lista las solicitudes creadas por un usuario.
     */
    Flux<VisitRequest> findByRequestedBy(Long userId);
    
    /**
     * Lista las solicitudes por estado.
     */
    Flux<VisitRequest> findByStatus(VisitStatus status);
    
    /**
     * Lista las solicitudes pendientes de una organización.
     */
    Flux<VisitRequest> findPendingByOrganization(Long organizationId);
    
    /**
     * Lista las solicitudes activas de una unidad.
     */
    Flux<VisitRequest> findActiveByUnit(Long unitId);
    
    /**
     * Guarda o actualiza una solicitud.
     */
    Mono<VisitRequest> save(VisitRequest visitRequest);
    
    /**
     * Elimina una solicitud.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Cuenta las solicitudes pendientes de una unidad.
     */
    Mono<Long> countPendingByUnit(Long unitId);
}
