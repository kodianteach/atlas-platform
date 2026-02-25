package co.com.atlas.usecase.invitation;

import co.com.atlas.model.auth.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Data Transfer Object for resident registration.
 * Contains the data submitted by a resident during self-registration via invitation token.
 * Similar to {@link OwnerRegistrationData} but without unit selection (unit comes from token).
 */
@Getter
@Builder
@AllArgsConstructor
public class ResidentRegistrationData {
    
    /**
     * Full name of the resident
     */
    private final String names;
    
    /**
     * Email address (optional)
     */
    private final String email;
    
    /**
     * Phone number
     */
    private final String phone;
    
    /**
     * Document type (default: CC - Cédula de Ciudadanía)
     */
    private final DocumentType documentType;
    
    /**
     * Document number
     */
    private final String documentNumber;
    
    /**
     * Raw password (will be encoded before storage)
     */
    private final String password;
    
    /**
     * Password confirmation for validation
     */
    private final String confirmPassword;
}
