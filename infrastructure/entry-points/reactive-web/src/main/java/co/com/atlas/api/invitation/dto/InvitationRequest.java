package co.com.atlas.api.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear invitación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest {
    private Long organizationId;
    private Long unitId;
    private String email;
    private String phoneNumber;
    private String type;
    private Long roleId;
}
