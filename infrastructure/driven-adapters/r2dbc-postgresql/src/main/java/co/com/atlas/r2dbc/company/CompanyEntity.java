package co.com.atlas.r2dbc.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para Company.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("company")
public class CompanyEntity {
    
    @Id
    private Long id;
    
    private String name;
    
    private String slug;
    
    @Column("tax_id")
    private String taxId;
    
    private String industry;
    
    private String website;
    
    private String address;
    
    private String country;
    
    private String city;
    
    private String status;
    
    @Column("is_active")
    private Boolean isActive;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
    
    @Column("deleted_at")
    private Instant deletedAt;
}
