package co.com.atlas.model.access.gateways;

import co.com.atlas.model.access.AccessScanLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de AccessScanLog.
 */
public interface AccessScanLogRepository {
    
    /**
     * Busca un log por ID.
     */
    Mono<AccessScanLog> findById(Long id);
    
    /**
     * Lista los logs de un código de acceso.
     */
    Flux<AccessScanLog> findByAccessCodeId(Long accessCodeId);
    
    /**
     * Lista los logs por usuario que escaneó.
     */
    Flux<AccessScanLog> findByScannedBy(Long userId);
    
    /**
     * Guarda un log de escaneo.
     */
    Mono<AccessScanLog> save(AccessScanLog scanLog);
    
    /**
     * Cuenta los escaneos exitosos de un código.
     */
    Mono<Long> countSuccessfulScans(Long accessCodeId);
}
