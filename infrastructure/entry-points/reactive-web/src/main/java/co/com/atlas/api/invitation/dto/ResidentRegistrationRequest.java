package co.com.atlas.api.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resident self-registration via invitation token.
 * Unit comes from the token, so no unitId field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentRegistrationRequest {
    private String token;
    private String names;
    private String phone;
    private String documentType;
    private String documentNumber;
    private String email;
    private String password;
    private String confirmPassword;
}
