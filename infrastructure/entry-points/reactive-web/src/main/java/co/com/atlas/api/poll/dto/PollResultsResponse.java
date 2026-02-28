package co.com.atlas.api.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta de resultados de encuesta con visibilidad condicional de votantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollResultsResponse {
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
    
    /**
     * Lista de votantes — solo visible para ADMIN_ATLAS y ADMIN.
     * Null para otros roles.
     */
    private List<VoterInfo> voters;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoterInfo {
        private Long userId;
        private Long optionId;
        private Instant votedAt;
    }
}
