package co.com.atlas.model.access;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para AccessScanLog (Log de escaneos en portería).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccessScanLog {
    private Long id;
    private Long accessCodeId;
    private Long scannedBy;
    private ScanResult scanResult;
    private String scanLocation;
    private String deviceInfo;
    private String notes;
    private Instant scannedAt;
    private Instant createdAt;
}
