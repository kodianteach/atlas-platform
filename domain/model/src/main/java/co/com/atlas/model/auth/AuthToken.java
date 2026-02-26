package co.com.atlas.model.auth;

import co.com.atlas.model.permission.Permission;
import co.com.atlas.model.permission.ModulePermission;
import co.com.atlas.model.role.Role;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuthToken {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Instant expiresAt;
    private Long userId;
    private String email;
    private String names;
    private List<Role> roles;
    private List<Permission> permissions;
    private List<ModulePermission> modulePermissions;
    
    /**
     * ID de la organización actual del usuario (multi-tenant).
     */
    private Long organizationId;
    
    /**
     * Nombre de la organización actual del usuario.
     */
    private String organizationName;
    
    /**
     * Lista de códigos de módulos habilitados para la organización actual.
     */
    private List<String> enabledModules;
    
    /**
     * Ruta por defecto calculada según roles y módulos habilitados.
     */
    private String defaultRoute;
}
