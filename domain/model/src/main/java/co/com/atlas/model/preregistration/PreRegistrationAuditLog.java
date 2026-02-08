package co.com.atlas.model.preregistration;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para registro de auditoría de pre-registros.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class PreRegistrationAuditLog {
    private Long id;
    private Long tokenId;
    private PreRegistrationAuditAction action;
    private Long performedBy;
    private String ipAddress;
    private String userAgent;
    private String details;
    private Instant createdAt;
}
