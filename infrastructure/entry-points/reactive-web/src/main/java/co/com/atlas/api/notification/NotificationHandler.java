package co.com.atlas.api.notification;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.notification.dto.NotificationResponse;
import co.com.atlas.model.notification.Notification;
import co.com.atlas.usecase.notification.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para endpoints de notificaciones in-app.
 */
@Component
@RequiredArgsConstructor
public class NotificationHandler {

    private final NotificationUseCase notificationUseCase;

    /**
     * Obtiene notificaciones de una organización.
     */
    public Mono<ServerResponse> findByOrganizationId(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return notificationUseCase.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(notifications -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(notifications, "Notificaciones obtenidas exitosamente")));
    }

    /**
     * Marca una notificación como leída.
     */
    public Mono<ServerResponse> markAsRead(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return notificationUseCase.markAsRead(id)
                .flatMap(notification -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(toResponse(notification), "Notificación marcada como leída")));
    }

    /**
     * Cuenta notificaciones no leídas.
     */
    public Mono<ServerResponse> countUnread(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return notificationUseCase.countUnread(organizationId)
                .flatMap(count -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(count, "Conteo obtenido exitosamente")));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .organizationId(notification.getOrganizationId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType() != null ? notification.getType().name() : null)
                .isRead(notification.getIsRead())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
