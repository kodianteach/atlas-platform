package co.com.atlas.model.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modelo de dominio para publicaciones (Posts).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private Long id;
    private Long organizationId;
    private Long authorId;
    private String title;
    private String content;
    private PostType type;
    private Boolean allowComments;
    private Boolean isPinned;
    private PostStatus status;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
