package co.com.atlas.api.external;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints públicos de autorización de visitantes.
 * <p>
 * Los endpoints bajo /api/external/** están configurados como permitAll
 * en SecurityConfig y no requieren autenticación JWT.
 * </p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/external/authorizations/{id}/qr-data  - Datos de verificación del QR</li>
 *   <li>GET /api/external/authorizations/{id}/qr-image - Imagen PNG del QR</li>
 * </ul>
 *
 * @author Atlas Platform Team
 * @since HU #6
 */
@Configuration
@Tag(name = "External Authorizations", description = "Verificación pública de autorizaciones de ingreso")
@RequiredArgsConstructor
public class ExternalAuthorizationRouterRest {

    private final ExternalAuthorizationHandler handler;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/external/authorizations/{id}/qr-data",
                    method = RequestMethod.GET,
                    beanClass = ExternalAuthorizationHandler.class,
                    beanMethod = "getQrData",
                    produces = "application/json"
            ),
            @RouterOperation(
                    path = "/api/external/authorizations/{id}/qr-image",
                    method = RequestMethod.GET,
                    beanClass = ExternalAuthorizationHandler.class,
                    beanMethod = "getQrImage",
                    produces = "image/png"
            )
    })
    public RouterFunction<ServerResponse> externalAuthorizationRouterFunction() {
        return route(GET("/api/external/authorizations/{id}/qr-data"), handler::getQrData)
                .andRoute(GET("/api/external/authorizations/{id}/qr-image"), handler::getQrImage);
    }
}
