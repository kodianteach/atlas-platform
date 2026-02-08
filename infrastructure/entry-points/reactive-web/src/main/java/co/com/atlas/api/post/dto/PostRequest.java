package co.com.atlas.api.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    private Long organizationId;
    private String title;
    private String content;
    private String type;
    private Boolean allowComments;
}
