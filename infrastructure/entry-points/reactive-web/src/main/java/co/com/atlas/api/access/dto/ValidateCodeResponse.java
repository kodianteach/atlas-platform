package co.com.atlas.api.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para validación de código.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCodeResponse {
    private Boolean valid;
    private String scanResult;
    private String visitorName;
    private String visitorDocument;
    private Long unitId;
    private Integer entriesUsed;
    private Integer maxEntries;
    private Instant validUntil;
    private String message;
}
