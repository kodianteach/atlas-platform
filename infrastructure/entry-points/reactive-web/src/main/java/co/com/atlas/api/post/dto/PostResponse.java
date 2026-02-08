package co.com.atlas.api.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private Long organizationId;
    private Long authorId;
    private String title;
    private String content;
    private String type;
    private Boolean allowComments;
    private Boolean isPinned;
    private String status;
    private Instant publishedAt;
    private Instant createdAt;
    private Long commentsCount;
}
