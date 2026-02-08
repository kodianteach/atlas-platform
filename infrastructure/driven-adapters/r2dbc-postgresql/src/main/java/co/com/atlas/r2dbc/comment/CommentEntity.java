package co.com.atlas.r2dbc.comment;

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
@Table("comments")
public class CommentEntity {

    @Id
    private Long id;

    @Column("post_id")
    private Long postId;

    @Column("author_id")
    private Long authorId;

    @Column("parent_id")
    private Long parentId;

    @Column("content")
    private String content;

    @Column("is_approved")
    private Boolean isApproved;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted_at")
    private Instant deletedAt;
}
