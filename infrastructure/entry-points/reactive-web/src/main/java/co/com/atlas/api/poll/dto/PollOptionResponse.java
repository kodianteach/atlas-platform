package co.com.atlas.api.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOptionResponse {
    private Long id;
    private String optionText;
    private Integer sortOrder;
    private Long voteCount;
    private Double percentage;
}
