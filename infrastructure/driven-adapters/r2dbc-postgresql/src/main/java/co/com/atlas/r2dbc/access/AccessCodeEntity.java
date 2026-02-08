package co.com.atlas.r2dbc.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para AccessCode.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("access_code")
public class AccessCodeEntity {
    
    @Id
    private Long id;
    
    @Column("visit_request_id")
    private Long visitRequestId;
    
    @Column("code_hash")
    private String codeHash;
    
    @Column("code_type")
    private String codeType;
    
    private String status;
    
    @Column("entries_used")
    private Integer entriesUsed;
    
    @Column("valid_from")
    private Instant validFrom;
    
    @Column("valid_until")
    private Instant validUntil;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
}
