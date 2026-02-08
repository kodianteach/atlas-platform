package co.com.atlas.r2dbc.post;

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
@Table("posts")
public class PostEntity {

    @Id
    private Long id;

    @Column("organization_id")
    private Long organizationId;

    @Column("author_id")
    private Long authorId;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("type")
    private String type;

    @Column("allow_comments")
    private Boolean allowComments;

    @Column("is_pinned")
    private Boolean isPinned;

    @Column("status")
    private String status;

    @Column("published_at")
    private Instant publishedAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted_at")
    private Instant deletedAt;
}
