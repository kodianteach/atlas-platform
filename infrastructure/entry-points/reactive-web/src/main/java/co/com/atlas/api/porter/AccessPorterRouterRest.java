package co.com.atlas.api.porter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para endpoints de validación de acceso en portería.
 * Prefijo: /api/porter
 */
@Configuration
@Tag(name = "Porter Access", description = "Validación de autorizaciones y gestión de accesos en portería")
public class AccessPorterRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/porter/validate-authorization",
                    method = RequestMethod.POST,
                    beanClass = AccessPorterHandler.class,
                    beanMethod = "validateAuthorization",
                    operation = @Operation(
                            operationId = "validateAuthorization",
                            summary = "Validar autorización QR online",
                            description = "Verifica firma Ed25519, rango de fechas, estado y registra evento de acceso",
                            tags = {"Porter Access"}
                    )
            ),
            @RouterOperation(
                    path = "/api/porter/validate-by-document",
                    method = RequestMethod.GET,
                    beanClass = AccessPorterHandler.class,
                    beanMethod = "findByDocument",
                    operation = @Operation(
                            operationId = "findByDocument",
                            summary = "Buscar autorizaciones por documento",
                            description = "Busca autorizaciones vigentes por número de documento de identidad",
                            tags = {"Porter Access"}
                    )
            ),
            @RouterOperation(
                    path = "/api/porter/validate-by-document",
                    method = RequestMethod.POST,
                    beanClass = AccessPorterHandler.class,
                    beanMethod = "validateByDocument",
                    operation = @Operation(
                            operationId = "validateByDocument",
                            summary = "Validar autorización por documento",
                            description = "Valida y registra acceso seleccionando una autorización encontrada por documento",
                            tags = {"Porter Access"}
                    )
            ),
            @RouterOperation(
                    path = "/api/porter/access-events/sync",
                    method = RequestMethod.POST,
                    beanClass = AccessPorterHandler.class,
                    beanMethod = "syncEvents",
                    operation = @Operation(
                            operationId = "syncAccessEvents",
                            summary = "Sincronizar eventos offline",
                            description = "Recibe lote de eventos generados offline y los persiste",
                            tags = {"Porter Access"}
                    )
            ),
            @RouterOperation(
                    path = "/api/porter/revocations",
                    method = RequestMethod.GET,
                    beanClass = AccessPorterHandler.class,
                    beanMethod = "getRevocations",
                    operation = @Operation(
                            operationId = "getRevocations",
                            summary = "Lista de revocaciones",
                            description = "Obtiene IDs de autorizaciones revocadas desde un timestamp",
                            tags = {"Porter Access"}
                    )
            ),
            @RouterOperation(
                    path = "/api/porter/vehicle-exit",
                    method = RequestMethod.POST,
                    beanClass = AccessPorterHandler.class,
                    beanMethod = "registerVehicleExit",
                    operation = @Operation(
                            operationId = "registerVehicleExit",
                            summary = "Registrar salida de vehículo",
                            description = "Registra la salida de un vehículo con placa y datos de la persona",
                            tags = {"Porter Access"}
                    )
            ),
            @RouterOperation(
                    path = "/api/porter/access-events",
                    method = RequestMethod.GET,
                    beanClass = AccessPorterHandler.class,
                    beanMethod = "getAccessEvents",
                    operation = @Operation(
                            operationId = "getAccessEvents",
                            summary = "Historial de eventos de acceso",
                            tags = {"Porter Access"}
                    )
            )
    })
    public RouterFunction<ServerResponse> accessPorterRoutes(AccessPorterHandler handler) {
        return route(POST("/api/porter/validate-authorization").and(accept(MediaType.APPLICATION_JSON)), handler::validateAuthorization)
                .andRoute(GET("/api/porter/validate-by-document"), handler::findByDocument)
                .andRoute(POST("/api/porter/validate-by-document").and(accept(MediaType.APPLICATION_JSON)), handler::validateByDocument)
                .andRoute(POST("/api/porter/access-events/sync").and(accept(MediaType.APPLICATION_JSON)), handler::syncEvents)
                .andRoute(GET("/api/porter/revocations"), handler::getRevocations)
                .andRoute(POST("/api/porter/vehicle-exit").and(accept(MediaType.APPLICATION_JSON)), handler::registerVehicleExit)
                .andRoute(GET("/api/porter/access-events"), handler::getAccessEvents);
    }
}
