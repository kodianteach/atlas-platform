package co.com.atlas.r2dbc.visit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para VisitApproval.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("visit_approval")
public class VisitApprovalEntity {
    
    @Id
    private Long id;
    
    @Column("visit_request_id")
    private Long visitRequestId;
    
    @Column("approved_by")
    private Long approvedBy;
    
    private String action;
    
    private String reason;
    
    @Column("created_at")
    private Instant createdAt;
}
