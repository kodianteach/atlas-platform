package co.com.atlas.api.porter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para validación de autorización por número de documento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateByDocumentRequest {
    private String personDocument;
    private Long authorizationId;
}
