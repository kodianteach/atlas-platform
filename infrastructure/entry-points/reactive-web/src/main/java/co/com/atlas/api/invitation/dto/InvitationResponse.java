package co.com.atlas.api.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para Invitation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    private Long id;
    private Long organizationId;
    private Long unitId;
    private String email;
    private String invitationToken;
    private String type;
    private Long roleId;
    private String status;
    private Long invitedBy;
    private Instant expiresAt;
    private Instant acceptedAt;
    private Instant createdAt;
}
