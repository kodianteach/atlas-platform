package co.com.atlas.r2dbc.access;

import co.com.atlas.model.access.AccessScanLog;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessScanLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway AccessScanLogRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class AccessScanLogRepositoryAdapter implements AccessScanLogRepository {

    private final AccessScanLogReactiveRepository repository;

    @Override
    public Mono<AccessScanLog> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<AccessScanLog> findByAccessCodeId(Long accessCodeId) {
        return repository.findByAccessCodeId(accessCodeId)
                .map(this::toDomain);
    }

    @Override
    public Flux<AccessScanLog> findByScannedBy(Long userId) {
        return repository.findByScannedBy(userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<AccessScanLog> save(AccessScanLog scanLog) {
        AccessScanLogEntity entity = toEntity(scanLog);
        if (entity.getScannedAt() == null) {
            entity.setScannedAt(Instant.now());
        }
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countSuccessfulScans(Long accessCodeId) {
        return repository.countByAccessCodeIdAndScanResult(accessCodeId, ScanResult.VALID.name());
    }

    private AccessScanLog toDomain(AccessScanLogEntity entity) {
        return AccessScanLog.builder()
                .id(entity.getId())
                .accessCodeId(entity.getAccessCodeId())
                .scannedBy(entity.getScannedBy())
                .scanResult(entity.getScanResult() != null ? ScanResult.valueOf(entity.getScanResult()) : null)
                .scanLocation(entity.getScanLocation())
                .deviceInfo(entity.getDeviceInfo())
                .scannedAt(entity.getScannedAt())
                .build();
    }

    private AccessScanLogEntity toEntity(AccessScanLog scanLog) {
        return AccessScanLogEntity.builder()
                .id(scanLog.getId())
                .accessCodeId(scanLog.getAccessCodeId())
                .scannedBy(scanLog.getScannedBy())
                .scanResult(scanLog.getScanResult() != null ? scanLog.getScanResult().name() : null)
                .scanLocation(scanLog.getScanLocation())
                .deviceInfo(scanLog.getDeviceInfo())
                .scannedAt(scanLog.getScannedAt())
                .build();
    }
}
