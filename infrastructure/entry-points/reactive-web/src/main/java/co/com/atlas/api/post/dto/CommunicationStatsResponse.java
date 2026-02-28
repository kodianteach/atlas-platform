package co.com.atlas.api.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO de respuesta para estadísticas de comunicaciones del panel admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationStatsResponse {
    private Map<String, Long> postsByStatus;
    private Map<String, Long> pollsByStatus;
    private long totalComments;
    private double participationRate;
}
