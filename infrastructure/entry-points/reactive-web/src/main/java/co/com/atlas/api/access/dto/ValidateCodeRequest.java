package co.com.atlas.api.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para validar código de acceso.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCodeRequest {
    private String code;
    private String scanLocation;
    private String deviceInfo;
}
