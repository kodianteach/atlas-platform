package co.com.atlas.model.access.gateways;

import co.com.atlas.model.access.AccessCode;
import co.com.atlas.model.access.AccessCodeStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de AccessCode.
 */
public interface AccessCodeRepository {
    
    /**
     * Busca un código por ID.
     */
    Mono<AccessCode> findById(Long id);
    
    /**
     * Busca un código por hash.
     */
    Mono<AccessCode> findByCodeHash(String codeHash);
    
    /**
     * Lista los códigos de una solicitud de visita.
     */
    Flux<AccessCode> findByVisitRequestId(Long visitRequestId);
    
    /**
     * Lista los códigos por estado.
     */
    Flux<AccessCode> findByStatus(AccessCodeStatus status);
    
    /**
     * Guarda o actualiza un código.
     */
    Mono<AccessCode> save(AccessCode accessCode);
    
    /**
     * Elimina un código.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si existe un código con el hash dado.
     */
    Mono<Boolean> existsByCodeHash(String codeHash);
}
