package co.com.atlas.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Value Object para estadísticas de comunicaciones del panel admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationStats {
    private Map<String, Long> postsByStatus;
    private Map<String, Long> pollsByStatus;
    private long totalComments;
    private double participationRate;
}
