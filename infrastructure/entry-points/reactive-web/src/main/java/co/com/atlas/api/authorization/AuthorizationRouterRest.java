package co.com.atlas.api.authorization;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints autenticados de autorización de visitantes.
 * <p>
 * Todos los endpoints requieren autenticación JWT.
 * El TenantContext (organizationId, userId, roles) es extraído del token por el JwtAuthenticationFilter.
 * </p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST   /api/authorizations          - Crear autorización (multipart: JSON + PDF)</li>
 *   <li>GET    /api/authorizations           - Listar autorizaciones (según rol)</li>
 *   <li>GET    /api/authorizations/{id}      - Consultar autorización por ID</li>
 *   <li>PUT    /api/authorizations/{id}/revoke - Revocar autorización</li>
 * </ul>
 *
 * @author Atlas Platform Team
 * @since HU #6
 */
@Configuration
@Tag(name = "Authorizations", description = "Gestión de autorizaciones de ingreso con QR firmado")
@RequiredArgsConstructor
public class AuthorizationRouterRest {

    private final AuthorizationHandler handler;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/authorizations",
                    method = RequestMethod.POST,
                    beanClass = AuthorizationHandler.class,
                    beanMethod = "create",
                    produces = "application/json",
                    consumes = "multipart/form-data"
            ),
            @RouterOperation(
                    path = "/api/authorizations",
                    method = RequestMethod.GET,
                    beanClass = AuthorizationHandler.class,
                    beanMethod = "getAll",
                    produces = "application/json"
            ),
            @RouterOperation(
                    path = "/api/authorizations/{id}",
                    method = RequestMethod.GET,
                    beanClass = AuthorizationHandler.class,
                    beanMethod = "getById",
                    produces = "application/json"
            ),
            @RouterOperation(
                    path = "/api/authorizations/{id}/revoke",
                    method = RequestMethod.PUT,
                    beanClass = AuthorizationHandler.class,
                    beanMethod = "revoke",
                    produces = "application/json"
            )
    })
    public RouterFunction<ServerResponse> authorizationRouterFunction() {
        return route(POST("/api/authorizations").and(contentType(MULTIPART_FORM_DATA)), handler::create)
                .andRoute(PUT("/api/authorizations/{id}/revoke"), handler::revoke)
                .andRoute(GET("/api/authorizations/{id}"), handler::getById)
                .andRoute(GET("/api/authorizations"), handler::getAll);
    }
}
