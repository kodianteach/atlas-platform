package co.com.atlas.model.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modelo de dominio para votos de encuesta.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {
    private Long id;
    private Long pollId;
    private Long optionId;
    private Long userId;
    private Instant createdAt;
}
