package co.com.atlas.api.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollResponse {
    private Long id;
    private Long organizationId;
    private Long authorId;
    private String title;
    private String description;
    private Boolean allowMultiple;
    private Boolean isAnonymous;
    private String status;
    private Instant startsAt;
    private Instant endsAt;
    private Instant createdAt;
    private List<PollOptionResponse> options;
    private Long totalVotes;
}
