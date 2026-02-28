package co.com.atlas.api.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para notificaciones in-app.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long organizationId;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private String entityType;
    private Long entityId;
    private Instant createdAt;
}
