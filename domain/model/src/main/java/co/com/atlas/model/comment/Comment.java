package co.com.atlas.model.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modelo de dominio para comentarios.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private Long id;
    private Long postId;
    private Long authorId;
    private Long parentId;
    private String content;
    private Boolean isApproved;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
