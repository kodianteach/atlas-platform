package co.com.atlas.model.visit.gateways;

import co.com.atlas.model.visit.VisitApproval;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de VisitApproval.
 */
public interface VisitApprovalRepository {
    
    /**
     * Busca una aprobación por ID.
     */
    Mono<VisitApproval> findById(Long id);
    
    /**
     * Lista las aprobaciones de una solicitud.
     */
    Flux<VisitApproval> findByVisitRequestId(Long visitRequestId);
    
    /**
     * Lista las aprobaciones de un aprobador.
     */
    Flux<VisitApproval> findByApprover(Long userId);
    
    /**
     * Busca la última aprobación de una solicitud.
     */
    Mono<VisitApproval> findLatestByVisitRequest(Long visitRequestId);
    
    /**
     * Guarda una aprobación.
     */
    Mono<VisitApproval> save(VisitApproval approval);
}
