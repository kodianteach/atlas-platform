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
 * Entidad de base de datos para AccessScanLog.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("access_scan_log")
public class AccessScanLogEntity {
    
    @Id
    private Long id;
    
    @Column("access_code_id")
    private Long accessCodeId;
    
    @Column("scanned_by")
    private Long scannedBy;
    
    @Column("scan_result")
    private String scanResult;
    
    @Column("scan_location")
    private String scanLocation;
    
    @Column("device_info")
    private String deviceInfo;
    
    @Column("scanned_at")
    private Instant scannedAt;
}
