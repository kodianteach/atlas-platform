package co.com.atlas.r2dbc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("channel_messages")
public class ChannelMessageEntity {

    @Id
    private Long id;

    @Column("organization_id")
    private Long organizationId;

    @Column("sender_id")
    private Long senderId;

    @Column("sender_name")
    private String senderName;

    @Column("sender_role")
    private String senderRole;

    @Column("content")
    private String content;

    @Column("status")
    private String status;

    @Column("is_edited")
    private Boolean isEdited;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted_at")
    private Instant deletedAt;
}
