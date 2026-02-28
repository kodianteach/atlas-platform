package co.com.atlas.api.notification;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router REST para notificaciones in-app.
 */
@Configuration
@Tag(name = "Notifications", description = "Gestión de notificaciones in-app")
public class NotificationRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/notifications/organization/{organizationId}",
                    method = RequestMethod.GET,
                    beanClass = NotificationHandler.class,
                    beanMethod = "findByOrganizationId",
                    operation = @Operation(
                            operationId = "getNotificationsByOrganization",
                            summary = "Obtener notificaciones de organización",
                            tags = {"Notifications"},
                            parameters = {
                                    @Parameter(name = "organizationId", in = ParameterIn.PATH, required = true,
                                            schema = @Schema(type = "integer"))
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Notificaciones obtenidas",
                                            content = @Content(schema = @Schema(implementation = NotificationResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/notifications/{id}/read",
                    method = RequestMethod.POST,
                    beanClass = NotificationHandler.class,
                    beanMethod = "markAsRead",
                    operation = @Operation(
                            operationId = "markNotificationAsRead",
                            summary = "Marcar notificación como leída",
                            tags = {"Notifications"},
                            parameters = {
                                    @Parameter(name = "id", in = ParameterIn.PATH, required = true,
                                            schema = @Schema(type = "integer"))
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Notificación actualizada"),
                                    @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/notifications/organization/{organizationId}/unread-count",
                    method = RequestMethod.GET,
                    beanClass = NotificationHandler.class,
                    beanMethod = "countUnread",
                    operation = @Operation(
                            operationId = "countUnreadNotifications",
                            summary = "Contar notificaciones no leídas",
                            tags = {"Notifications"},
                            parameters = {
                                    @Parameter(name = "organizationId", in = ParameterIn.PATH, required = true,
                                            schema = @Schema(type = "integer"))
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Conteo obtenido")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> notificationRoutes(NotificationHandler handler) {
        return route(GET("/api/notifications/organization/{organizationId}"), handler::findByOrganizationId)
                .andRoute(POST("/api/notifications/{id}/read"), handler::markAsRead)
                .andRoute(GET("/api/notifications/organization/{organizationId}/unread-count"), handler::countUnread);
    }
}
