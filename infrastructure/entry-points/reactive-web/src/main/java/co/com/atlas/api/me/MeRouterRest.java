package co.com.atlas.api.me;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints del usuario autenticado (/api/me).
 */
@Configuration
public class MeRouterRest {

    @Bean
    public RouterFunction<ServerResponse> meRoutes(MeHandler handler) {
        return route(GET("/api/me/residence"), handler::getMyResidence);
    }
}
