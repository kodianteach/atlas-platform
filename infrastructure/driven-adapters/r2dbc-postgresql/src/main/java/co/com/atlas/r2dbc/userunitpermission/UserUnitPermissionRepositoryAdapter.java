package co.com.atlas.r2dbc.userunitpermission;

import co.com.atlas.model.userunitpermission.UserUnitPermission;
import co.com.atlas.model.userunitpermission.gateways.UserUnitPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway UserUnitPermissionRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class UserUnitPermissionRepositoryAdapter implements UserUnitPermissionRepository {

    private final UserUnitPermissionReactiveRepository repository;

    @Override
    public Mono<UserUnitPermission> save(UserUnitPermission permission) {
        UserUnitPermissionEntity entity = toEntity(permission);
        if (entity.getGrantedAt() == null) {
            entity.setGrantedAt(Instant.now());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity).map(this::toDomain);
    }

    @Override
    public Flux<UserUnitPermission> findByUserUnitId(Long userUnitId) {
        return repository.findByUserUnitId(userUnitId).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteByUserUnitId(Long userUnitId) {
        return repository.deleteByUserUnitId(userUnitId);
    }

    private UserUnitPermission toDomain(UserUnitPermissionEntity entity) {
        return UserUnitPermission.builder()
                .id(entity.getId())
                .userUnitId(entity.getUserUnitId())
                .permissionId(entity.getPermissionId())
                .grantedBy(entity.getGrantedBy())
                .grantedAt(entity.getGrantedAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private UserUnitPermissionEntity toEntity(UserUnitPermission permission) {
        return UserUnitPermissionEntity.builder()
                .id(permission.getId())
                .userUnitId(permission.getUserUnitId())
                .permissionId(permission.getPermissionId())
                .grantedBy(permission.getGrantedBy())
                .grantedAt(permission.getGrantedAt())
                .expiresAt(permission.getExpiresAt())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
