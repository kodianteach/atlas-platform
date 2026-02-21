package co.com.atlas.r2dbc.porter;

import co.com.atlas.model.porter.PorterEnrollmentAuditAction;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway PorterEnrollmentAuditRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class PorterEnrollmentAuditRepositoryAdapter implements PorterEnrollmentAuditRepository {

    private final PorterEnrollmentAuditReactiveRepository repository;

    @Override
    public Mono<PorterEnrollmentAuditLog> save(PorterEnrollmentAuditLog auditLog) {
        PorterEnrollmentAuditEntity entity = toEntity(auditLog);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Flux<PorterEnrollmentAuditLog> findByTokenId(Long tokenId) {
        return repository.findByTokenId(tokenId)
                .map(this::toDomain);
    }

    private PorterEnrollmentAuditLog toDomain(PorterEnrollmentAuditEntity entity) {
        return PorterEnrollmentAuditLog.builder()
                .id(entity.getId())
                .tokenId(entity.getTokenId())
                .action(PorterEnrollmentAuditAction.valueOf(entity.getAction()))
                .performedBy(entity.getPerformedBy())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .details(entity.getDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PorterEnrollmentAuditEntity toEntity(PorterEnrollmentAuditLog log) {
        return PorterEnrollmentAuditEntity.builder()
                .id(log.getId())
                .tokenId(log.getTokenId())
                .action(log.getAction() != null ? log.getAction().name() : null)
                .performedBy(log.getPerformedBy())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
