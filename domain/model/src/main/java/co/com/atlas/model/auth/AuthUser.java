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
public class AuthUser {
    private Long id;
    private String names;
    private String email;
    private String username;
    private String passwordHash;
    private String phone;
    
    /**
     * Tipo de documento de identificación.
     * Requerido para nuevos registros de usuario.
     */
    private DocumentType documentType;
    
    /**
     * Número de documento de identificación.
     * Único por tipo de documento.
     */
    private String documentNumber;
    
    private boolean active;
    
    /**
     * Estado del usuario en el sistema.
     * Controla el flujo de pre-registro y activación.
     */
    private UserStatus status;
    
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Role> roles;
    private List<Permission> permissions;
    private List<ModulePermission> modulePermissions;
    
    /**
     * ID de la última organización seleccionada por el usuario.
     * Usado para auto-selección en futuros logins.
     */
    private Long lastOrganizationId;
    
    /**
     * ID de la organización actual del usuario (multi-tenant).
     * Usado para tenant isolation en JWT claims.
     */
    private Long organizationId;
    
    /**
     * Lista de códigos de módulos habilitados para la organización actual.
     * Ejemplo: ["ATLAS_CORE"]
     */
    private List<String> enabledModules;
}
