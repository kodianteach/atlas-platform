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
@Table("polls")
public class PollEntity {

    @Id
    private Long id;

    @Column("organization_id")
    private Long organizationId;

    @Column("author_id")
    private Long authorId;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("allow_multiple")
    private Boolean allowMultiple;

    @Column("is_anonymous")
    private Boolean isAnonymous;

    @Column("status")
    private String status;

    @Column("starts_at")
    private Instant startsAt;

    @Column("ends_at")
    private Instant endsAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted_at")
    private Instant deletedAt;
}
