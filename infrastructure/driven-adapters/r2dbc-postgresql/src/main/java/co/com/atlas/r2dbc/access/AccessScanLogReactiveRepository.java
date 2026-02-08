package co.com.atlas.r2dbc.access;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para AccessScanLog.
 */
public interface AccessScanLogReactiveRepository extends ReactiveCrudRepository<AccessScanLogEntity, Long> {
    
    Flux<AccessScanLogEntity> findByAccessCodeId(Long accessCodeId);
    
    Flux<AccessScanLogEntity> findByScannedBy(Long userId);
    
    Mono<Long> countByAccessCodeIdAndScanResult(Long accessCodeId, String scanResult);
}
