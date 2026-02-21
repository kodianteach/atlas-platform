package co.com.atlas.api.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a resident invitation.
 * Contains optional permissions map when owner permission management is enabled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResidentInvitationRequest {
    /**
     * Optional permissions to assign to the resident.
     * Keys: create, edit, delete, view
     * Values: true/false
     */
    private Map<String, Boolean> permissions;
}
