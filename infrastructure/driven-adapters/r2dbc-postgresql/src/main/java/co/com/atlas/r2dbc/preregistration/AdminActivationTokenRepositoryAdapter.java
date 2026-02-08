package co.com.atlas.r2dbc.preregistration;

import co.com.atlas.model.preregistration.AdminActivationToken;
import co.com.atlas.model.preregistration.ActivationTokenStatus;
import co.com.atlas.model.preregistration.gateways.AdminActivationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter R2DBC para AdminActivationTokenRepository.
 */
@Repository
@RequiredArgsConstructor
public class AdminActivationTokenRepositoryAdapter implements AdminActivationTokenRepository {
    
    private final AdminActivationTokenReactiveRepository repository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public Mono<AdminActivationToken> findById(Long id) {
        return repository.findById(id).map(this::toModel);
    }
    
    @Override
    public Mono<AdminActivationToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toModel);
    }
    
    @Override
    public Flux<AdminActivationToken> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(this::toModel);
    }
    
    @Override
    public Mono<AdminActivationToken> findLatestPendingByUserId(Long userId) {
        return repository.findLatestPendingByUserId(userId).map(this::toModel);
    }
    
    @Override
    public Flux<AdminActivationToken> findByStatus(ActivationTokenStatus status) {
        return repository.findByStatus(status.name()).map(this::toModel);
    }
    
    @Override
    public Flux<AdminActivationToken> findExpiredPendingTokens() {
        return repository.findExpiredPendingTokens().map(this::toModel);
    }
    
    @Override
    public Mono<AdminActivationToken> save(AdminActivationToken token) {
        AdminActivationTokenEntity entity = toEntity(token);
        
        // Si initialPasswordHash no parece un hash BCrypt, hashearlo
        if (entity.getInitialPasswordHash() != null 
                && !entity.getInitialPasswordHash().startsWith("$2")) {
            entity.setInitialPasswordHash(passwordEncoder.encode(entity.getInitialPasswordHash()));
        }
        
        return repository.save(entity).map(this::toModel);
    }
    
    @Override
    public Mono<Boolean> existsValidTokenForUser(Long userId) {
        return repository.existsValidTokenForUser(userId);
    }
    
    private AdminActivationToken toModel(AdminActivationTokenEntity entity) {
        return AdminActivationToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .initialPasswordHash(entity.getInitialPasswordHash())
                .expiresAt(entity.getExpiresAt())
                .consumedAt(entity.getConsumedAt())
                .status(ActivationTokenStatus.valueOf(entity.getStatus()))
                .createdBy(entity.getCreatedBy())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .activationIp(entity.getActivationIp())
                .activationUserAgent(entity.getActivationUserAgent())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private AdminActivationTokenEntity toEntity(AdminActivationToken model) {
        return AdminActivationTokenEntity.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .tokenHash(model.getTokenHash())
                .initialPasswordHash(model.getInitialPasswordHash())
                .expiresAt(model.getExpiresAt())
                .consumedAt(model.getConsumedAt())
                .status(model.getStatus() != null ? model.getStatus().name() : null)
                .createdBy(model.getCreatedBy())
                .ipAddress(model.getIpAddress())
                .userAgent(model.getUserAgent())
                .activationIp(model.getActivationIp())
                .activationUserAgent(model.getActivationUserAgent())
                .metadata(model.getMetadata())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
