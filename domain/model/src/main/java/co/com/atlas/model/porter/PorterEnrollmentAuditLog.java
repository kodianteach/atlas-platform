package co.com.atlas.model.porter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para auditoría de enrolamiento de porteros.
 * Sigue el patrón de preregistration_audit_log (V5).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class PorterEnrollmentAuditLog {
    private Long id;
    private Long tokenId;
    private PorterEnrollmentAuditAction action;
    private Long performedBy;
    private String ipAddress;
    private String userAgent;
    private String details;
    private Instant createdAt;
}
