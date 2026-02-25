package co.com.atlas.api.me;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para la residencia del usuario autenticado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyResidenceResponse {
    private String organizationName;
    private String unitCode;
    private String ownershipType;
    private String roleName;
}
