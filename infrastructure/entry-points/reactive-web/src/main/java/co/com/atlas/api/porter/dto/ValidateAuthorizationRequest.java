package co.com.atlas.api.porter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para validación online de autorización QR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateAuthorizationRequest {
    private String signedQr;
}
