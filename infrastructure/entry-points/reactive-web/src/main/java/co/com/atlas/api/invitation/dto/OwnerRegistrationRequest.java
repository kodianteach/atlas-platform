package co.com.atlas.api.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for owner self-registration via invitation token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRegistrationRequest {
    private String token;
    private String names;
    private String phone;
    private String documentType;
    private String documentNumber;
    private String email;
    private String password;
    private String confirmPassword;
    private Long unitId;
}
