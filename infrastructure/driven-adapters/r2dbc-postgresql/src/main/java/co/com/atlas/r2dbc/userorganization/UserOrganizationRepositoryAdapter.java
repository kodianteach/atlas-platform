package co.com.atlas.r2dbc.userorganization;

import co.com.atlas.model.userorganization.UserOrganization;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway UserOrganizationRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class UserOrganizationRepositoryAdapter implements UserOrganizationRepository {

    private final UserOrganizationReactiveRepository repository;
    private final DatabaseClient databaseClient;
    private static final String ACTIVE_STATUS = "ACTIVE";

    @Override
    public Mono<UserOrganization> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserOrganization> findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserOrganization> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserOrganization> findActiveByUserId(Long userId) {
        return repository.findByUserIdAndStatus(userId, ACTIVE_STATUS)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserOrganization> findByUserIdAndOrganizationId(Long userId, Long organizationId) {
        return repository.findByUserIdAndOrganizationId(userId, organizationId)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserOrganization> save(UserOrganization userOrganization) {
        UserOrganizationEntity entity = toEntity(userOrganization);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Boolean> existsByUserIdAndOrganizationId(Long userId, Long organizationId) {
        return repository.existsByUserIdAndOrganizationId(userId, organizationId);
    }

    @Override
    public Mono<Long> countActiveByOrganization(Long organizationId) {
        return repository.countByOrganizationIdAndStatus(organizationId, ACTIVE_STATUS);
    }

    private UserOrganization toDomain(UserOrganizationEntity entity) {
        return UserOrganization.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .organizationId(entity.getOrganizationId())
                .status(entity.getStatus())
                .joinedAt(entity.getJoinedAt())
                .leftAt(entity.getLeftAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserOrganizationEntity toEntity(UserOrganization userOrganization) {
        return UserOrganizationEntity.builder()
                .id(userOrganization.getId())
                .userId(userOrganization.getUserId())
                .organizationId(userOrganization.getOrganizationId())
                .status(userOrganization.getStatus())
                .joinedAt(userOrganization.getJoinedAt())
                .leftAt(userOrganization.getLeftAt())
                .createdAt(userOrganization.getCreatedAt())
                .updatedAt(userOrganization.getUpdatedAt())
                .build();
    }
    
    @Override
    public Mono<Void> updateStatusByUserIdAndOrganizationId(Long userId, Long organizationId, String status) {
        return databaseClient.sql("""
            UPDATE user_organizations 
            SET status = :status, updated_at = :now 
            WHERE user_id = :userId AND organization_id = :organizationId
            """)
                .bind("status", status)
                .bind("now", Instant.now())
                .bind("userId", userId)
                .bind("organizationId", organizationId)
                .then();
    }
}
