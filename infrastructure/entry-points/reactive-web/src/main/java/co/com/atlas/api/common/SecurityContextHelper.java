package co.com.atlas.api.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Utility for extracting authenticated user context in WebFlux handlers.
 * Uses ReactiveSecurityContextHolder (reactive context propagation) instead of
 * ThreadLocal-based TenantContext, which is unreliable in non-blocking pipelines.
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    /**
     * Extracts userId, organizationId, and roles from the reactive security context.
     * The JwtAuthenticationFilter stores:
     * - userId as the Authentication principal (String)
     * - organizationId in authentication.details Map under key "organizationId"
     * - roles as GrantedAuthority (prefixed with "ROLE_")
     */
    public static Mono<AuthContext> extractAuthContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(SecurityContextHelper::buildAuthContext)
                .defaultIfEmpty(AuthContext.empty());
    }

    @SuppressWarnings("unchecked")
    private static AuthContext buildAuthContext(Authentication authentication) {
        // userId is stored as the principal (String)
        Long userId = null;
        if (authentication.getPrincipal() instanceof String principal) {
            try {
                userId = Long.parseLong(principal);
            } catch (NumberFormatException ignored) {}
        }

        // organizationId is stored in details Map
        Long organizationId = null;
        if (authentication.getDetails() instanceof Map<?, ?> details) {
            Object orgId = details.get("organizationId");
            if (orgId instanceof Long l) {
                organizationId = l;
            } else if (orgId instanceof Integer i) {
                organizationId = i.longValue();
            }
        }

        // roles from authorities
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();

        return new AuthContext(userId, organizationId, roles);
    }

    /**
     * Holds authenticated user context extracted from ReactiveSecurityContextHolder.
     */
    public record AuthContext(Long userId, Long organizationId, List<String> roles) {
        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }

        public static AuthContext empty() {
            return new AuthContext(null, null, List.of());
        }
    }
}
