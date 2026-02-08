package co.com.atlas.r2dbc.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("poll_votes")
public class PollVoteEntity {

    @Id
    private Long id;

    @Column("poll_id")
    private Long pollId;

    @Column("option_id")
    private Long optionId;

    @Column("user_id")
    private Long userId;

    @Column("created_at")
    private Instant createdAt;
}
