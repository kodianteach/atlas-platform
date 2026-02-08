package co.com.atlas.api.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollRequest {
    private Long organizationId;
    private String title;
    private String description;
    private Boolean allowMultiple;
    private Boolean isAnonymous;
    private List<String> options;
}
