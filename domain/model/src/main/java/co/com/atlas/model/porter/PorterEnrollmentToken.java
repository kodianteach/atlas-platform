package co.com.atlas.model.porter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Token de Enrolamiento de Portero.
 * Sigue el patrón de admin_activation_tokens (V5).
 * Token de un solo uso con expiración para enrolar dispositivos de portería.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class PorterEnrollmentToken {
    private Long id;
    private Long userId;
    private Long organizationId;
    private String tokenHash;
    private PorterEnrollmentTokenStatus status;
    private Instant expiresAt;
    private Instant consumedAt;
    private Long createdBy;
    private String ipAddress;
    private String userAgent;
    private String activationIp;
    private String activationUserAgent;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Verifica si el token ha expirado por tiempo.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Verifica si el token es válido (pendiente y no expirado).
     */
    public boolean isValid() {
        return status == PorterEnrollmentTokenStatus.PENDING && !isExpired();
    }
}
