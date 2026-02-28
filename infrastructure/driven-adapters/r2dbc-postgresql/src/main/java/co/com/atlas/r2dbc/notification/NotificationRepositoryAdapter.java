package co.com.atlas.r2dbc.notification;

import co.com.atlas.model.notification.Notification;
import co.com.atlas.model.notification.NotificationType;
import co.com.atlas.model.notification.gateways.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Adaptador de repositorio para notificaciones in-app.
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationReactiveRepository repository;

    @Override
    public Mono<Notification> save(Notification notification) {
        NotificationEntity entity = toEntity(notification);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity).map(this::toDomain);
    }

    @Override
    public Flux<Notification> findByOrganizationIdAndUserId(Long organizationId, Long userId) {
        return repository.findByOrganizationIdAndUserId(organizationId, userId).map(this::toDomain);
    }

    @Override
    public Flux<Notification> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId).map(this::toDomain);
    }

    @Override
    public Mono<Notification> markAsRead(Long id) {
        return repository.findById(id)
                .flatMap(entity -> {
                    entity.setIsRead(true);
                    return repository.save(entity);
                })
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countUnreadByOrganizationId(Long organizationId) {
        return repository.countUnreadByOrganizationId(organizationId);
    }

    private Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType() != null ? NotificationType.valueOf(entity.getType()) : null)
                .isRead(entity.getIsRead())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private NotificationEntity toEntity(Notification domain) {
        return NotificationEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .userId(domain.getUserId())
                .title(domain.getTitle())
                .message(domain.getMessage())
                .type(domain.getType() != null ? domain.getType().name() : null)
                .isRead(domain.getIsRead())
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
