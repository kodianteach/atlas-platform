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
    private String phoneNumber;
    private String invitationToken;
    private InvitationType type;
    private Long roleId;
    private String initialPermissions; // JSON como String
    private InvitationStatus status;
    private Long invitedBy;
    private Instant expiresAt;
    private Instant acceptedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
