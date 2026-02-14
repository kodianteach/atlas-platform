package co.com.atlas.r2dbc.role;

import co.com.atlas.model.role.Role;
import co.com.atlas.model.role.gateways.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Implementación del gateway RoleRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleReactiveRepository repository;

    @Override
    public Mono<Role> findByCode(String code) {
        return repository.findByCode(code)
                .map(this::toDomain);
    }

    @Override
    public Mono<Role> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    private Role toDomain(RoleEntity entity) {
        return Role.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .moduleCode(entity.getModuleCode())
                .isSystem(entity.getIsSystem())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
