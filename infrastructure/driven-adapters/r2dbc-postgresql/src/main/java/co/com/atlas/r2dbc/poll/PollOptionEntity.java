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
@Table("poll_options")
public class PollOptionEntity {

    @Id
    private Long id;

    @Column("poll_id")
    private Long pollId;

    @Column("option_text")
    private String optionText;

    @Column("sort_order")
    private Integer sortOrder;

    @Column("created_at")
    private Instant createdAt;
}
