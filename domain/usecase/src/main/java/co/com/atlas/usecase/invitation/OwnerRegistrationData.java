package co.com.atlas.usecase.invitation;

import co.com.atlas.model.auth.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Datos de registro del propietario al aceptar invitación.
 */
@Getter
@Builder
@AllArgsConstructor
public class OwnerRegistrationData {
    private String names;
    private String phone;
    private DocumentType documentType;
    private String documentNumber;
    private String email;
    private String password;
}
