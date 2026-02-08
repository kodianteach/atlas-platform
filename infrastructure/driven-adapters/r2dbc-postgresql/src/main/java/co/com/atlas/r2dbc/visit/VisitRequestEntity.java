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
 * Entidad de base de datos para VisitRequest.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("visit_request")
public class VisitRequestEntity {
    
    @Id
    private Long id;
    
    @Column("organization_id")
    private Long organizationId;
    
    @Column("unit_id")
    private Long unitId;
    
    @Column("requested_by")
    private Long requestedBy;
    
    @Column("visitor_name")
    private String visitorName;
    
    @Column("visitor_document")
    private String visitorDocument;
    
    @Column("visitor_phone")
    private String visitorPhone;
    
    @Column("visitor_email")
    private String visitorEmail;
    
    private String reason;
    
    @Column("valid_from")
    private Instant validFrom;
    
    @Column("valid_until")
    private Instant validUntil;
    
    @Column("recurrence_type")
    private String recurrenceType;
    
    @Column("max_entries")
    private Integer maxEntries;
    
    private String status;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
