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
@Table("message_read_status")
public class MessageReadStatusEntity {

    @Id
    private Long id;

    @Column("message_id")
    private Long messageId;

    @Column("user_id")
    private Long userId;

    @Column("read_at")
    private Instant readAt;
}
