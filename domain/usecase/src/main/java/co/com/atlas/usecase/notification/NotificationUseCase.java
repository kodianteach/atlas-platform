package co.com.atlas.usecase.notification;

import co.com.atlas.model.notification.Notification;
import co.com.atlas.model.notification.NotificationType;
import co.com.atlas.model.notification.gateways.NotificationRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Caso de uso para gestión de notificaciones in-app.
 */
@RequiredArgsConstructor
public class NotificationUseCase {

    private final NotificationRepository notificationRepository;

    /**
     * Crea una notificación broadcast para toda la organización.
     */
    public Mono<Notification> createForOrganization(Long organizationId, String title, String message,
                                                     NotificationType type, String entityType, Long entityId) {
        Notification notification = Notification.builder()
                .organizationId(organizationId)
                .userId(null)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(Instant.now())
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * Obtiene notificaciones de una organización.
     */
    public Flux<Notification> findByOrganizationId(Long organizationId) {
        return notificationRepository.findByOrganizationId(organizationId);
    }

    /**
     * Marca una notificación como leída.
     */
    public Mono<Notification> markAsRead(Long id) {
        return notificationRepository.markAsRead(id);
    }

    /**
     * Cuenta notificaciones no leídas.
     */
    public Mono<Long> countUnread(Long organizationId) {
        return notificationRepository.countUnreadByOrganizationId(organizationId);
    }
}
