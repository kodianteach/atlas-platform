package co.com.atlas.r2dbc.notification;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para notificaciones in-app.
 */
public interface NotificationReactiveRepository extends ReactiveCrudRepository<NotificationEntity, Long> {

    @Query("SELECT * FROM notifications WHERE organization_id = :organizationId ORDER BY created_at DESC")
    Flux<NotificationEntity> findByOrganizationId(Long organizationId);

    @Query("SELECT * FROM notifications WHERE organization_id = :organizationId AND user_id = :userId ORDER BY created_at DESC")
    Flux<NotificationEntity> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE organization_id = :organizationId AND is_read = FALSE")
    Mono<Long> countUnreadByOrganizationId(Long organizationId);
}
