package co.com.atlas.model.invitation;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Invitation (Invitaciones por token).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Invitation {
    private Long id;
    private Long organizationId;
    private Long unitId;
    private String email;
    private String invitationToken;
    private InvitationType type;
    private Long roleId;
    private String initialPermissions; // JSON como String
    private InvitationStatus status;
    private Long invitedBy;
    private Instant expiresAt;
    
    /**
     * Timestamp cuando se envió la invitación por email.
     */
    private Instant invitationSentAt;
    
    /**
     * Estado del envío de la invitación: PENDING, SENT, FAILED.
     */
    private String invitationMailStatus;
    
    /**
     * Número de reintentos de envío realizados.
     */
    private Integer retryCount;
    
    /**
     * Timestamp del último reintento de envío.
     */
    private Instant lastRetryAt;
    
    /**
     * Metadata adicional en formato JSON.
     */
    private String metadata;
    
    private Instant acceptedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
