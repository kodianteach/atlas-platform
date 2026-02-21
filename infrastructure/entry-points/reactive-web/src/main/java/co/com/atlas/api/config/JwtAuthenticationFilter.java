package co.com.atlas.api.config;

import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import co.com.atlas.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Filtro WebFlux para autenticación JWT.
 * Valida el token Bearer en el header Authorization y establece el contexto de seguridad.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/api/users/register",
            "/api/external",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/actuator"
    );

    private final JwtTokenGateway jwtTokenGateway;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        log.debug("JWT Filter - Processing request: {} {}", method, path);
        
        if (isPublicPath(path)) {
            log.debug("JWT Filter - Public path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("JWT Filter - Missing or invalid Authorization header for path: {}", path);
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return jwtTokenGateway.validateToken(token)
                .onErrorResume(e -> {
                    log.error("JWT Filter - Error validating token: {}", e.getMessage());
                    return Mono.just(false);
                })
                .flatMap(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        log.debug("JWT Filter - Token valid for path: {}", path);
                        // Set TenantContext HERE (same thread as handler will run on)
                        setTenantContext(token);
                        return createAuthentication(token)
                                .flatMap(auth -> chain.filter(exchange)
                                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                                        .doFinally(signal -> TenantContext.clear()));
                    }
                    log.warn("JWT Filter - Token invalid for path: {}", path);
                    return unauthorized(exchange);
                });
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<UsernamePasswordAuthenticationToken> createAuthentication(String token) {
        return Mono.zip(
                jwtTokenGateway.extractUserId(token),
                jwtTokenGateway.extractRole(token)
        ).map(tuple -> {
            String userId = tuple.getT1();
            String role = tuple.getT2();
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // Store organizationId in details so handlers can retrieve it reactively
            Long orgId = extractOrganizationIdFromToken(token);
            if (orgId != null) {
                auth.setDetails(Map.of("organizationId", orgId));
            }
            return auth;
        });
    }

    /**
     * Extracts organizationId claim from JWT token.
     */
    private Long extractOrganizationIdFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object orgIdClaim = claims.get("organizationId");
            if (orgIdClaim instanceof Integer) {
                return ((Integer) orgIdClaim).longValue();
            } else if (orgIdClaim instanceof Long) {
                return (Long) orgIdClaim;
            }
        } catch (Exception e) {
            log.warn("Failed to extract organizationId from token: {}", e.getMessage());
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Extrae userId y organizationId del JWT y los establece en TenantContext.
     * Se ejecuta en el mismo hilo donde correrá el handler (post-validación async).
     */
    private void setTenantContext(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            if (subject != null) {
                TenantContext.setUserId(Long.valueOf(subject));
            }

            Object orgIdClaim = claims.get("organizationId");
            if (orgIdClaim instanceof Integer) {
                TenantContext.setOrganizationId(((Integer) orgIdClaim).longValue());
            } else if (orgIdClaim instanceof Long) {
                TenantContext.setOrganizationId((Long) orgIdClaim);
            }

            log.debug("JWT Filter - TenantContext set: userId={}, organizationId={}",
                    TenantContext.getUserId(), TenantContext.getOrganizationId());
        } catch (Exception e) {
            log.warn("JWT Filter - Failed to set TenantContext: {}", e.getMessage());
        }
    }
}
