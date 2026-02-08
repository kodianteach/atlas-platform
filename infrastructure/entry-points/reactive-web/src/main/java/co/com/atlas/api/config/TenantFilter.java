package co.com.atlas.api.config;

import co.com.atlas.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WebFilter responsable de extraer información del tenant desde tokens JWT
 * y configurarla en el TenantContext para la solicitud actual.
 * <p>
 * Este filtro corre DESPUÉS de la autenticación y extrae:
 * <ul>
 *   <li>userId - del claim subject del JWT</li>
 *   <li>organizationId - del claim personalizado del JWT</li>
 * </ul>
 * </p>
 * 
 * <p>El filtro limpia automáticamente el TenantContext después del procesamiento
 * para prevenir contaminación de contexto y memory leaks.</p>
 * 
 * <p><strong>Order:</strong> Corre después de JwtAuthenticationFilter (order 0) para asegurar
 * que el token ya está validado.</p>
 * 
 * @see TenantContext
 * @see JwtAuthenticationFilter
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Correr después del filtro de seguridad
public class TenantFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/api/users/register",
            "/api/invitations",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/actuator"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        log.debug("TenantFilter - Processing request: {}", path);

        // Saltar configuración de tenant context para rutas públicas
        if (isPublicPath(path)) {
            log.debug("TenantFilter - Public path, skipping tenant context: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("TenantFilter - No Authorization header found for protected path: {}", path);
            // Dejarlo pasar - JwtAuthenticationFilter manejará la autenticación
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Parsear claims del JWT
            Claims claims = parseToken(token);
            
            // Extraer información del tenant
            Long userId = extractUserId(claims);
            Long organizationId = extractOrganizationId(claims);

            if (userId != null && organizationId != null) {
                // Establecer contexto del tenant
                TenantContext.setUserId(userId);
                TenantContext.setOrganizationId(organizationId);
                
                log.debug("TenantFilter - Tenant context set: userId={}, organizationId={}", 
                         userId, organizationId);
            } else {
                log.warn("TenantFilter - Missing tenant claims in token: userId={}, organizationId={}", 
                        userId, organizationId);
            }

            // Continuar cadena de filtros y asegurar limpieza
            return chain.filter(exchange)
                    .doFinally(signalType -> {
                        TenantContext.clear();
                        log.debug("TenantFilter - Tenant context cleared (signal: {})", signalType);
                    });

        } catch (Exception e) {
            log.error("TenantFilter - Error extracting tenant context from token: {}", e.getMessage(), e);
            // Limpiar contexto en error y continuar (auth filter manejará tokens inválidos)
            TenantContext.clear();
            return chain.filter(exchange);
        }
    }

    /**
     * Parsea el token JWT y extrae los claims.
     * 
     * @param token el string del token JWT
     * @return objeto Claims parseado
     * @throws io.jsonwebtoken.JwtException si el token es inválido
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el user ID de los claims del JWT.
     * 
     * @param claims los claims del JWT
     * @return user ID como Long, o null si no está presente o es inválido
     */
    private Long extractUserId(Claims claims) {
        try {
            String subject = claims.getSubject();
            return subject != null ? Long.valueOf(subject) : null;
        } catch (NumberFormatException e) {
            log.warn("TenantFilter - Invalid userId in token subject: {}", claims.getSubject());
            return null;
        }
    }

    /**
     * Extrae el organization ID de los claims del JWT.
     * <p>
     * Busca el claim 'organizationId' en el payload del JWT.
     * Este claim debe ser establecido durante la generación del token en JwtTokenAdapter.
     * </p>
     * 
     * @param claims los claims del JWT
     * @return organization ID como Long, o null si no está presente o es inválido
     */
    private Long extractOrganizationId(Claims claims) {
        try {
            Object orgIdClaim = claims.get("organizationId");
            if (orgIdClaim == null) {
                log.debug("TenantFilter - No organizationId claim found in token");
                return null;
            }
            
            // Manejar tipos Integer y Long
            if (orgIdClaim instanceof Integer) {
                return ((Integer) orgIdClaim).longValue();
            } else if (orgIdClaim instanceof Long) {
                return (Long) orgIdClaim;
            } else if (orgIdClaim instanceof String) {
                return Long.valueOf((String) orgIdClaim);
            } else {
                log.warn("TenantFilter - Unexpected organizationId type: {}", orgIdClaim.getClass());
                return null;
            }
        } catch (Exception e) {
            log.warn("TenantFilter - Error extracting organizationId from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica si la ruta del request es pública y no requiere contexto de tenant.
     * 
     * @param path la ruta del request
     * @return true si la ruta es pública, false en caso contrario
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
