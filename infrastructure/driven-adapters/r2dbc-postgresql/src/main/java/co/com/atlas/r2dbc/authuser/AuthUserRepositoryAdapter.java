package co.com.atlas.r2dbc.authuser;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.permission.Permission;
import co.com.atlas.model.role.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Implementación del gateway AuthUserRepository usando R2DBC.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuthUserRepositoryAdapter implements AuthUserRepository {

    private final AuthUserReactiveRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<AuthUser> findByEmail(String email) {
        return repository.findByEmailAndDeletedAtIsNull(email)
                .flatMap(this::enrichWithRolesAndPermissions);
    }

    @Override
    public Mono<AuthUser> findById(Long id) {
        return repository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .flatMap(this::enrichWithRolesAndPermissions);
    }

    @Override
    public Mono<Boolean> validatePassword(String rawPassword, String encodedPassword) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Override
    public Mono<AuthUser> save(AuthUser user) {
        AuthUserEntity entity = toEntity(user);
        if (user.getPasswordHash() != null && !user.getPasswordHash().startsWith("$2a$")) {
            entity.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        return repository.save(entity)
                .flatMap(this::enrichWithRolesAndPermissions);
    }

    @Override
    public Mono<Void> updateLastLogin(Long userId) {
        return databaseClient.sql("UPDATE users SET last_login_at = :lastLoginAt WHERE id = :userId")
                .bind("lastLoginAt", Instant.now())
                .bind("userId", userId)
                .then();
    }

    @Override
    public Flux<Role> findRolesByUserIdAndOrganizationId(Long userId, Long organizationId) {
        String sql = """
            SELECT r.id, r.name, r.code, r.description, r.module_code, r.is_system
            FROM role r
            JOIN user_roles_multi urm ON urm.role_id = r.id
            WHERE urm.user_id = :userId AND urm.organization_id = :organizationId
            """;
        
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .bind("organizationId", organizationId)
                .map((row, metadata) -> Role.builder()
                        .id(row.get("id", Long.class))
                        .name(row.get("name", String.class))
                        .code(row.get("code", String.class))
                        .description(row.get("description", String.class))
                        .moduleCode(row.get("module_code", String.class))
                        .isSystem(Boolean.TRUE.equals(row.get("is_system", Boolean.class)))
                        .build())
                .all();
    }

    @Override
    public Flux<Permission> findPermissionsByUserIdAndOrganizationId(Long userId, Long organizationId) {
        String sql = """
            SELECT DISTINCT p.id, p.code, p.name, p.description, p.module_code, p.resource, p.action
            FROM permissions p
            JOIN role_permissions rp ON rp.permission_id = p.id
            JOIN user_roles_multi urm ON urm.role_id = rp.role_id
            WHERE urm.user_id = :userId AND urm.organization_id = :organizationId
            """;
        
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .bind("organizationId", organizationId)
                .map((row, metadata) -> Permission.builder()
                        .id(row.get("id", Long.class))
                        .code(row.get("code", String.class))
                        .name(row.get("name", String.class))
                        .description(row.get("description", String.class))
                        .moduleCode(row.get("module_code", String.class))
                        .resource(row.get("resource", String.class))
                        .action(row.get("action", String.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Void> updateLastOrganization(Long userId, Long organizationId) {
        return databaseClient.sql("UPDATE users SET last_organization_id = :organizationId WHERE id = :userId")
                .bind("organizationId", organizationId)
                .bind("userId", userId)
                .then();
    }

    private Mono<AuthUser> enrichWithRolesAndPermissions(AuthUserEntity entity) {
        // Si tiene lastOrganizationId, cargar roles y permisos de esa organización
        if (entity.getLastOrganizationId() != null) {
            Long orgId = entity.getLastOrganizationId();
            return Mono.zip(
                    findRolesByUserIdAndOrganizationId(entity.getId(), orgId).collectList(),
                    findPermissionsByUserIdAndOrganizationId(entity.getId(), orgId).collectList()
            ).map(tuple -> toDomain(entity, tuple.getT1(), tuple.getT2(), orgId));
        }
        
        // Sin organización, retornar usuario básico
        return Mono.just(toDomain(entity, new ArrayList<>(), new ArrayList<>(), null));
    }

    private AuthUser toDomain(AuthUserEntity entity, java.util.List<Role> roles, 
                              java.util.List<Permission> permissions, Long organizationId) {
        UserStatus userStatus = null;
        if (entity.getStatus() != null) {
            try {
                userStatus = UserStatus.valueOf(entity.getStatus());
            } catch (IllegalArgumentException e) {
                userStatus = UserStatus.ACTIVE;
            }
        }
        
        return AuthUser.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .names(entity.getNames())
                .passwordHash(entity.getPasswordHash())
                .phone(entity.getPhone())
                .active(entity.isActive())
                .status(userStatus)
                .organizationId(organizationId)
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    private AuthUserEntity toEntity(AuthUser user) {
        return AuthUserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .names(user.getNames())
                .passwordHash(user.getPasswordHash())
                .phone(user.getPhone())
                .active(user.isActive())
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .lastOrganizationId(user.getOrganizationId())
                .build();
    }
}
