package co.com.atlas.model.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modelo de dominio para opciones de encuesta.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PollOption {
    private Long id;
    private Long pollId;
    private String optionText;
    private Integer sortOrder;
    private Instant createdAt;
    
    // Transient for aggregates
    private Long voteCount;
}
