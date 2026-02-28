package co.com.atlas.r2dbc.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de BD para notificaciones in-app.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class NotificationEntity {

    @Id
    private Long id;

    @Column("organization_id")
    private Long organizationId;

    @Column("user_id")
    private Long userId;

    private String title;
    private String message;
    private String type;

    @Column("is_read")
    private Boolean isRead;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private Long entityId;

    @Column("created_at")
    private Instant createdAt;
}
