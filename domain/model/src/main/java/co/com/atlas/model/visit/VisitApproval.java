package co.com.atlas.model.visit;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para VisitApproval (Aprobaciones de visita).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class VisitApproval {
    private Long id;
    private Long visitRequestId;
    private Long approvedBy;
    private ApprovalAction action;
    private String reason;
    private Instant createdAt;
}
