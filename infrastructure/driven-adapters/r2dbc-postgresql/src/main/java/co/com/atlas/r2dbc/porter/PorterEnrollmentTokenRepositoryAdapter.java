package co.com.atlas.r2dbc.porter;

import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway PorterEnrollmentTokenRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class PorterEnrollmentTokenRepositoryAdapter implements PorterEnrollmentTokenRepository {

    private final PorterEnrollmentTokenReactiveRepository repository;

    @Override
    public Mono<PorterEnrollmentToken> save(PorterEnrollmentToken token) {
        PorterEnrollmentTokenEntity entity = toEntity(token);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<PorterEnrollmentToken> findActiveByUserId(Long userId) {
        return repository.findByUserIdAndStatus(userId, PorterEnrollmentTokenStatus.PENDING.name())
                .map(this::toDomain);
    }

    @Override
    public Mono<PorterEnrollmentToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    private PorterEnrollmentToken toDomain(PorterEnrollmentTokenEntity entity) {
        return PorterEnrollmentToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .organizationId(entity.getOrganizationId())
                .tokenHash(entity.getTokenHash())
                .status(PorterEnrollmentTokenStatus.valueOf(entity.getStatus()))
                .expiresAt(entity.getExpiresAt())
                .consumedAt(entity.getConsumedAt())
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

    private PorterEnrollmentTokenEntity toEntity(PorterEnrollmentToken token) {
        return PorterEnrollmentTokenEntity.builder()
                .id(token.getId())
                .userId(token.getUserId())
                .organizationId(token.getOrganizationId())
                .tokenHash(token.getTokenHash())
                .status(token.getStatus() != null ? token.getStatus().name() : null)
                .expiresAt(token.getExpiresAt())
                .consumedAt(token.getConsumedAt())
                .createdBy(token.getCreatedBy())
                .ipAddress(token.getIpAddress())
                .userAgent(token.getUserAgent())
                .activationIp(token.getActivationIp())
                .activationUserAgent(token.getActivationUserAgent())
                .metadata(token.getMetadata())
                .createdAt(token.getCreatedAt())
                .updatedAt(token.getUpdatedAt())
                .build();
    }
}
