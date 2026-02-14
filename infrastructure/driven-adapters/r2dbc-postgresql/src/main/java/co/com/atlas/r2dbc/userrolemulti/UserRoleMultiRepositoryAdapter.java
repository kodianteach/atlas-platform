package co.com.atlas.r2dbc.userrolemulti;

import co.com.atlas.model.userrolemulti.UserRoleMulti;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway UserRoleMultiRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class UserRoleMultiRepositoryAdapter implements UserRoleMultiRepository {

    private final UserRoleMultiReactiveRepository repository;

    @Override
    public Mono<UserRoleMulti> save(UserRoleMulti userRoleMulti) {
        UserRoleMultiEntity entity = toEntity(userRoleMulti);
        Instant now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        if (entity.getAssignedAt() == null) {
            entity.setAssignedAt(now);
        }
        entity.setUpdatedAt(now);
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserRoleMulti> findByUserIdAndOrganizationId(Long userId, Long organizationId) {
        return repository.findByUserIdAndOrganizationId(userId, organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserRoleMulti> findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByUserIdAndOrganizationIdAndRoleId(Long userId, Long organizationId, Long roleId) {
        return repository.existsByUserIdAndOrganizationIdAndRoleId(userId, organizationId, roleId);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteByUserIdAndOrganizationId(Long userId, Long organizationId) {
        return repository.deleteByUserIdAndOrganizationId(userId, organizationId);
    }

    @Override
    public Flux<UserRoleMulti> findByUserIdAndOrganizationIdIsNull(Long userId) {
        return repository.findByUserIdAndOrganizationIdIsNull(userId)
                .map(this::toDomain);
    }

    private UserRoleMulti toDomain(UserRoleMultiEntity entity) {
        return UserRoleMulti.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .organizationId(entity.getOrganizationId())
                .roleId(entity.getRoleId())
                .isPrimary(entity.getIsPrimary())
                .assignedAt(entity.getAssignedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserRoleMultiEntity toEntity(UserRoleMulti domain) {
        return UserRoleMultiEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .organizationId(domain.getOrganizationId())
                .roleId(domain.getRoleId())
                .isPrimary(domain.getIsPrimary())
                .assignedAt(domain.getAssignedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
