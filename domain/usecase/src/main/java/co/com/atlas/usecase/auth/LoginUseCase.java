package co.com.atlas.usecase.auth;

import co.com.atlas.model.auth.AuthCredentials;
import co.com.atlas.model.auth.AuthToken;
import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import co.com.atlas.model.permission.Permission;
import co.com.atlas.model.permission.gateways.PermissionRepository;
import co.com.atlas.model.role.Role;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Caso de uso para el login de usuarios.
 */
@RequiredArgsConstructor
public class LoginUseCase {
    
    private final AuthUserRepository authUserRepository;
    private final JwtTokenGateway jwtTokenGateway;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserRoleMultiRepository userRoleMultiRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public Mono<AuthToken> execute(AuthCredentials credentials) {
        return authUserRepository.findByEmail(credentials.getEmail())
                .switchIfEmpty(Mono.error(new AuthenticationException("Usuario no encontrado")))
                .flatMap(user -> validateUserAndPassword(user, credentials.getPassword()))
                .flatMap(this::loadUserRoles)
                .flatMap(user -> authUserRepository.updateLastLogin(user.getId())
                        .then(jwtTokenGateway.generateTokenPair(user)));
    }

    private Mono<AuthUser> validateUserAndPassword(AuthUser user, String password) {
        if (!user.isActive()) {
            return Mono.error(new AuthenticationException("Usuario inactivo"));
        }
        return authUserRepository.validatePassword(password, user.getPasswordHash())
                .flatMap(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        return Mono.just(user);
                    }
                    return Mono.error(new AuthenticationException("Credenciales inválidas"));
                });
    }
    
    /**
     * Carga los roles del usuario desde user_roles_multi.
     * Usa la última organización seleccionada o la primera organización activa.
     * Si no tiene organizaciones, busca roles con organization_id = NULL
     * (asignados durante pre-registro, antes del onboarding).
     */
    private Mono<AuthUser> loadUserRoles(AuthUser user) {
        // Obtener la organización a usar (lastOrganizationId o primera activa)
        Mono<Long> organizationIdMono = user.getLastOrganizationId() != null
                ? Mono.just(user.getLastOrganizationId())
                : userOrganizationRepository.findActiveByUserId(user.getId())
                        .next()
                        .map(uo -> uo.getOrganizationId())
                        .defaultIfEmpty(0L);
        
        return organizationIdMono.flatMap(orgId -> {
            if (orgId == 0L) {
                // Usuario sin organizaciones, buscar roles sin organización asignada
                // (roles asignados durante pre-registro)
                return userRoleMultiRepository.findByUserIdAndOrganizationIdIsNull(user.getId())
                        .flatMap(urm -> roleRepository.findById(urm.getRoleId()))
                        .collectList()
                        .flatMap(roles -> loadPermissionsForRoles(roles)
                                .map(permissions -> user.toBuilder()
                                        .roles(roles)
                                        .permissions(permissions)
                                        .build()));
            }
            
            // Cargar roles del usuario en esa organización
            return userRoleMultiRepository.findByUserIdAndOrganizationId(user.getId(), orgId)
                    .flatMap(urm -> roleRepository.findById(urm.getRoleId()))
                    .collectList()
                    .flatMap(roles -> loadPermissionsForRoles(roles)
                            .map(permissions -> user.toBuilder()
                                    .organizationId(orgId)
                                    .roles(roles)
                                    .permissions(permissions)
                                    .build()));
        });
    }
    
    /**
     * Carga todos los permisos asociados a una lista de roles.
     * Los permisos se obtienen de la tabla role_permissions.
     * 
     * @param roles Lista de roles
     * @return Mono con lista de permisos únicos
     */
    private Mono<List<Permission>> loadPermissionsForRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Mono.just(List.of());
        }
        
        return Flux.fromIterable(roles)
                .flatMap(role -> permissionRepository.findByRoleId(role.getId()))
                .distinct(Permission::getCode) // Evitar duplicados por código
                .collectList();
    }
}
