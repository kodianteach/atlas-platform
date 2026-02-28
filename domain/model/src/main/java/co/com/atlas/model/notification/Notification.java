package co.com.atlas.model.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para notificaciones in-app.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Notification {
    private Long id;
    private Long organizationId;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private String entityType;
    private Long entityId;
    private Instant createdAt;
}
