package co.com.atlas.r2dbc.preregistration;

import co.com.atlas.model.preregistration.PreRegistrationAuditAction;
import co.com.atlas.model.preregistration.PreRegistrationAuditLog;
import co.com.atlas.model.preregistration.gateways.PreRegistrationAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter R2DBC para PreRegistrationAuditRepository.
 */
@Repository
@RequiredArgsConstructor
public class PreRegistrationAuditRepositoryAdapter implements PreRegistrationAuditRepository {
    
    private final PreRegistrationAuditLogReactiveRepository repository;
    
    @Override
    public Mono<PreRegistrationAuditLog> save(PreRegistrationAuditLog audit) {
        PreRegistrationAuditLogEntity entity = toEntity(audit);
        return repository.save(entity).map(this::toModel);
    }
    
    @Override
    public Flux<PreRegistrationAuditLog> findByTokenId(Long tokenId) {
        return repository.findByTokenId(tokenId).map(this::toModel);
    }
    
    @Override
    public Flux<PreRegistrationAuditLog> findByAction(PreRegistrationAuditAction action) {
        return repository.findByAction(action.name()).map(this::toModel);
    }
    
    private PreRegistrationAuditLog toModel(PreRegistrationAuditLogEntity entity) {
        return PreRegistrationAuditLog.builder()
                .id(entity.getId())
                .tokenId(entity.getTokenId())
                .action(PreRegistrationAuditAction.valueOf(entity.getAction()))
                .performedBy(entity.getPerformedBy())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .details(entity.getDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private PreRegistrationAuditLogEntity toEntity(PreRegistrationAuditLog model) {
        return PreRegistrationAuditLogEntity.builder()
                .id(model.getId())
                .tokenId(model.getTokenId())
                .action(model.getAction() != null ? model.getAction().name() : null)
                .performedBy(model.getPerformedBy())
                .ipAddress(model.getIpAddress())
                .userAgent(model.getUserAgent())
                .details(model.getDetails())
                .createdAt(model.getCreatedAt())
                .build();
    }
}
