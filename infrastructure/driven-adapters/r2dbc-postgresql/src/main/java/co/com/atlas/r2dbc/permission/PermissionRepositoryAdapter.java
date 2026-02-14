package co.com.atlas.r2dbc.permission;

import co.com.atlas.model.permission.Permission;
import co.com.atlas.model.permission.gateways.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementación del gateway PermissionRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionReactiveRepository repository;

    @Override
    public Mono<Permission> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }
    
    @Override
    public Mono<Permission> findByCode(String code) {
        return repository.findByCode(code)
                .map(this::toDomain);
    }

    @Override
    public Flux<Permission> findByRoleId(Long roleId) {
        return repository.findByRoleId(roleId)
                .map(this::toDomain);
    }

    private Permission toDomain(PermissionEntity entity) {
        return Permission.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .moduleCode(entity.getModuleCode())
                .resource(entity.getResource())
                .action(entity.getAction())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
