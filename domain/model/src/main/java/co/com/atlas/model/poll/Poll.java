package co.com.atlas.model.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Modelo de dominio para encuestas.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Poll {
    private Long id;
    private Long organizationId;
    private Long authorId;
    private String title;
    private String description;
    private Boolean allowMultiple;
    private Boolean isAnonymous;
    private PollStatus status;
    private Instant startsAt;
    private Instant endsAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    
    // Transient for aggregates
    private List<PollOption> options;
}
