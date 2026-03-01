package co.com.atlas.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración de seguridad WebFlux para la plataforma Atlas.
 * Define las rutas públicas, autenticación JWT y configuración de filtros.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // Deshabilitar todos los headers de seguridad para permitir Swagger UI
                .headers(ServerHttpSecurity.HeaderSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Permitir todas las peticiones OPTIONS (preflight CORS)
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints públicos de autenticación
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login/multi-tenant").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/select-organization").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/verify-token").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        // Endpoints públicos de usuarios
                        .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        // Endpoints de vehículos (requieren autenticación)
                        .pathMatchers("/api/vehicles/**").authenticated()
                        .pathMatchers("/api/units/**").authenticated()
                        // Endpoints de visitas (requieren autenticación)
                        .pathMatchers("/api/visits/**").authenticated()
                        // Endpoints de autorizaciones (requieren autenticación)
                        .pathMatchers("/api/authorizations/**").authenticated()
                        // Endpoints públicos de invitaciones (validar, aceptar y reenviar)
                        .pathMatchers("/api/invitations/**").permitAll()
                        // Endpoints externos de pre-registro de administradores
                        .pathMatchers("/api/external/**").permitAll()
                        // Swagger/OpenAPI
                        .pathMatchers("/swagger-ui.html").permitAll()
                        .pathMatchers("/swagger-ui/**").permitAll()
                        .pathMatchers("/api-docs").permitAll()
                        .pathMatchers("/api-docs/**").permitAll()
                        .pathMatchers("/v3/api-docs/**").permitAll()
                        .pathMatchers("/webjars/**").permitAll()
                        .pathMatchers("/favicon.ico").permitAll()
                        // Actuator
                        .pathMatchers("/actuator/**").permitAll()
                        // WebSocket endpoint (auth validated in handshake)
                        .pathMatchers("/ws/**").permitAll()
                        // Todos los demás endpoints requieren autenticación
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
