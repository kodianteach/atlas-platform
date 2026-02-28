package co.com.atlas.model.notification.gateways;

import co.com.atlas.model.notification.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para repositorio de notificaciones in-app.
 */
public interface NotificationRepository {

    /**
     * Guarda una notificación.
     */
    Mono<Notification> save(Notification notification);

    /**
     * Busca notificaciones por organización y usuario.
     */
    Flux<Notification> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    /**
     * Busca notificaciones broadcast de una organización (userId null).
     */
    Flux<Notification> findByOrganizationId(Long organizationId);

    /**
     * Marca una notificación como leída.
     */
    Mono<Notification> markAsRead(Long id);

    /**
     * Cuenta notificaciones no leídas por organización.
     */
    Mono<Long> countUnreadByOrganizationId(Long organizationId);
}
