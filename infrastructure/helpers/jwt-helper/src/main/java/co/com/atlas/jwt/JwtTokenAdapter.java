package co.com.atlas.jwt;

import co.com.atlas.jwt.config.JwtProperties;
import co.com.atlas.model.auth.AuthToken;
import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.permission.Permission;
import co.com.atlas.model.permission.ModulePermission;
import co.com.atlas.model.role.Role;
import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements JwtTokenGateway {

    private final JwtProperties jwtProperties;

    @Override
    public Mono<String> generateAccessToken(AuthUser user) {
        return Mono.fromCallable(() -> buildToken(user, jwtProperties.getAccessTokenExpiration()));
    }

    @Override
    public Mono<String> generateRefreshToken(AuthUser user) {
        return Mono.fromCallable(() -> buildRefreshToken(user, jwtProperties.getRefreshTokenExpiration()));
    }

    @Override
    public Mono<AuthToken> generateTokenPair(AuthUser user) {
        return Mono.zip(generateAccessToken(user), generateRefreshToken(user))
                .map(tuple -> AuthToken.builder()
                        .accessToken(tuple.getT1())
                        .refreshToken(tuple.getT2())
                        .tokenType("Bearer")
                        .expiresAt(Instant.now().plusMillis(jwtProperties.getAccessTokenExpiration()))
                        .userId(user.getId())
                        .email(user.getEmail())
                        .names(user.getNames())
                        .roles(user.getRoles())
                        .permissions(user.getPermissions())
                        .modulePermissions(user.getModulePermissions())
                        .build());
    }

    @Override
    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token);
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        });
    }

    @Override
    public Mono<String> extractUserId(String token) {
        return Mono.fromCallable(() -> getClaims(token).getSubject());
    }

    @Override
    public Mono<String> extractRole(String token) {
        return Mono.fromCallable(() -> {
            @SuppressWarnings("unchecked")
            List<String> roles = getClaims(token).get("roles", List.class);
            return roles != null && !roles.isEmpty() ? roles.get(0) : "";
        });
    }

    @Override
    public Mono<AuthToken> refreshToken(String refreshToken) {
        return Mono.fromCallable(() -> {
            Claims claims = getClaims(refreshToken);
            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            String names = claims.get("names", String.class);

            @SuppressWarnings("unchecked")
            List<String> roleNames = claims.get("roles", List.class);

            @SuppressWarnings("unchecked")
            List<String> permissionNames = claims.get("permissions", List.class);

            List<Role> roles = roleNames != null 
                    ? roleNames.stream()
                        .map(code -> Role.builder().code(code).build())
                        .collect(Collectors.toList())
                    : List.of();

            List<Permission> permissions = permissionNames != null 
                    ? permissionNames.stream()
                        .map(code -> Permission.builder().code(code).build())
                        .collect(Collectors.toList())
                    : List.of();

            AuthUser user = AuthUser.builder()
                    .id(userId)
                    .email(email)
                    .names(names)
                    .roles(roles)
                    .permissions(permissions)
                    .build();

            return user;
        }).flatMap(this::generateTokenPair);
    }

    private String buildToken(AuthUser user, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        // Usar códigos (enums) en lugar de nombres para validación frontend/backend
        List<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream()
                    .map(Role::getCode)
                    .collect(Collectors.toList())
                : List.of();

        List<String> permissionCodes = user.getPermissions() != null
                ? user.getPermissions().stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toList())
                : List.of();

        // Construcción de permisos granulares por módulo
        List<Map<String, Object>> modulePermissions = user.getModulePermissions() != null
                ? user.getModulePermissions().stream()
                    .map(mp -> {
                        Map<String, Object> permMap = new HashMap<>();
                        permMap.put("moduleId", mp.getModuleId());
                        permMap.put("moduleName", mp.getModuleName());
                        permMap.put("moduleRoute", mp.getModuleRoute());
                        permMap.put("viewId", mp.getViewId());
                        permMap.put("viewName", mp.getViewName());
                        permMap.put("permissionCode", mp.getPermissionCode());
                        permMap.put("permissionName", mp.getPermissionName());
                        return permMap;
                    })
                    .collect(Collectors.toList())
                : List.of();

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail() != null ? user.getEmail() : "");
        claims.put("names", user.getNames() != null ? user.getNames() : "");
        claims.put("roles", roleCodes);
        claims.put("permissions", permissionCodes);
        claims.put("modulePermissions", modulePermissions);
        
        // MULTI-TENANT: Agregar organizationId (CRÍTICO para tenant isolation)
        if (user.getOrganizationId() != null) {
            claims.put("organizationId", user.getOrganizationId());
        }
        
        // MULTI-TENANT: Agregar módulos habilitados para esta organización
        if (user.getEnabledModules() != null && !user.getEnabledModules().isEmpty()) {
            claims.put("enabledModules", user.getEnabledModules());
        }
        
        // MULTI-TENANT: Calcular y agregar ruta por defecto según roles y módulos
        String defaultRoute = calculateDefaultRoute(user.getRoles(), user.getEnabledModules());
        if (defaultRoute != null) {
            claims.put("defaultRoute", defaultRoute);
        }

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(claims)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    private String buildRefreshToken(AuthUser user, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        // Usar códigos (enums) en lugar de nombres para validación frontend/backend
        List<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream()
                    .map(Role::getCode)
                    .collect(Collectors.toList())
                : List.of();

        List<String> permissionCodes = user.getPermissions() != null
                ? user.getPermissions().stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toList())
                : List.of();

        List<Map<String, Object>> modulePermissions = user.getModulePermissions() != null
                ? user.getModulePermissions().stream()
                    .map(mp -> {
                        Map<String, Object> permMap = new HashMap<>();
                        permMap.put("moduleId", mp.getModuleId());
                        permMap.put("moduleName", mp.getModuleName());
                        permMap.put("moduleRoute", mp.getModuleRoute());
                        permMap.put("viewId", mp.getViewId());
                        permMap.put("viewName", mp.getViewName());
                        permMap.put("permissionCode", mp.getPermissionCode());
                        permMap.put("permissionName", mp.getPermissionName());
                        return permMap;
                    })
                    .collect(Collectors.toList())
                : List.of();

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail() != null ? user.getEmail() : "");
        claims.put("names", user.getNames() != null ? user.getNames() : "");
        claims.put("roles", roleCodes);
        claims.put("permissions", permissionCodes);
        claims.put("modulePermissions", modulePermissions);
        claims.put("type", "refresh");
        
        // MULTI-TENANT: Agregar organizationId en refresh token también
        if (user.getOrganizationId() != null) {
            claims.put("organizationId", user.getOrganizationId());
        }
        
        // MULTI-TENANT: Agregar módulos habilitados
        if (user.getEnabledModules() != null && !user.getEnabledModules().isEmpty()) {
            claims.put("enabledModules", user.getEnabledModules());
        }

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(claims)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Calcula la ruta por defecto del usuario según sus roles y módulos habilitados.
     * 
     * @param roles lista de roles del usuario
     * @param enabledModules lista de códigos de módulos habilitados
     * @return ruta por defecto, nunca null
     */
    private String calculateDefaultRoute(List<Role> roles, List<String> enabledModules) {
        if (roles == null || roles.isEmpty()) {
            return "/profile";
        }
        
        // Extraer nombres de roles
        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        
        // SUPER_ADMIN siempre va al dashboard principal
        if (roleNames.contains("SUPER_ADMIN")) {
            return "/dashboard";
        }
        
        // Si no hay módulos habilitados, ir a perfil
        if (enabledModules == null || enabledModules.isEmpty()) {
            return "/profile";
        }
        
        // Para Atlas: roles de administración de organizaciones residenciales
        if (roleNames.contains("ADMIN_ATLAS") && enabledModules.contains("ATLAS_CORE")) {
            return "/dashboard";
        }
        
        if (roleNames.contains("OWNER") && enabledModules.contains("ATLAS_CORE")) {
            return "/my-unit";
        }
        
        if (roleNames.contains("TENANT") && enabledModules.contains("ATLAS_CORE")) {
            return "/my-unit";
        }
        
        if (roleNames.contains("SECURITY") && enabledModules.contains("ATLAS_CORE")) {
            return "/access-control";
        }
        
        // Fallback
        return "/dashboard";
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
