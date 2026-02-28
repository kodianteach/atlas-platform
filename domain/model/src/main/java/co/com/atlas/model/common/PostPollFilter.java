package co.com.atlas.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Value Object para filtrado dinámico de publicaciones y encuestas en panel admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostPollFilter {
    private String type;
    private String status;
    private Instant dateFrom;
    private Instant dateTo;
    private String search;
    private int page;
    private int size;
}
