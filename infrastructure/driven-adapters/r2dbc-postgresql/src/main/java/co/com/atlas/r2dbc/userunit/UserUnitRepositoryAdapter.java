package co.com.atlas.r2dbc.userunit;

import co.com.atlas.model.userunit.OwnershipType;
import co.com.atlas.model.userunit.UserUnit;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway UserUnitRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class UserUnitRepositoryAdapter implements UserUnitRepository {

    private final UserUnitReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<UserUnit> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserUnit> findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserUnit> findByUnitId(Long unitId) {
        return repository.findByUnitId(unitId)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserUnit> findActiveByUserId(Long userId) {
        return repository.findByUserIdAndStatusActive(userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserUnit> findByUserIdAndUnitId(Long userId, Long unitId) {
        return repository.findByUserIdAndUnitId(userId, unitId)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserUnit> findPrimaryByUserId(Long userId) {
        return repository.findByUserIdAndIsPrimaryTrue(userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserUnit> save(UserUnit userUnit) {
        UserUnitEntity entity = toEntity(userUnit);
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
    public Mono<Boolean> existsByUserIdAndUnitId(Long userId, Long unitId) {
        return repository.existsByUserIdAndUnitId(userId, unitId);
    }

    @Override
    public Mono<Long> countActiveByUnit(Long unitId) {
        return repository.countByUnitIdAndStatusActive(unitId);
    }

    private UserUnit toDomain(UserUnitEntity entity) {
        return UserUnit.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .unitId(entity.getUnitId())
                .roleId(entity.getRoleId())
                .ownershipType(entity.getOwnershipType() != null ? OwnershipType.valueOf(entity.getOwnershipType()) : null)
                .isPrimary(entity.getIsPrimary())
                .moveInDate(entity.getMoveInDate())
                .status(entity.getStatus())
                .invitedBy(entity.getInvitedBy())
                .joinedAt(entity.getJoinedAt())
                .isActive("ACTIVE".equalsIgnoreCase(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private UserUnitEntity toEntity(UserUnit userUnit) {
        return UserUnitEntity.builder()
                .id(userUnit.getId())
                .userId(userUnit.getUserId())
                .unitId(userUnit.getUnitId())
                .roleId(userUnit.getRoleId())
                .ownershipType(userUnit.getOwnershipType() != null ? userUnit.getOwnershipType().name() : null)
                .isPrimary(userUnit.getIsPrimary())
                .moveInDate(userUnit.getMoveInDate())
                .status(userUnit.getStatus() != null ? userUnit.getStatus() : "ACTIVE")
                .invitedBy(userUnit.getInvitedBy())
                .joinedAt(userUnit.getJoinedAt() != null ? userUnit.getJoinedAt() : Instant.now())
                .createdAt(userUnit.getCreatedAt())
                .updatedAt(userUnit.getUpdatedAt())
                .deletedAt(userUnit.getDeletedAt())
                .build();
    }
    
    @Override
    public Mono<Void> updateStatusByUserIdAndUnitId(Long userId, Long unitId, String status) {
        return databaseClient.sql("""
            UPDATE user_units 
            SET status = :status, updated_at = :now 
            WHERE user_id = :userId AND unit_id = :unitId
            """)
                .bind("status", status)
                .bind("now", Instant.now())
                .bind("userId", userId)
                .bind("unitId", unitId)
                .then();
    }
}
