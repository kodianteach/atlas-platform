package co.com.atlas.api.visit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para aprobar/rechazar visita.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitApprovalDto {
    private String action;
    private String reason;
}
