package co.com.atlas.model.access;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para AccessCode (Códigos de acceso QR).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccessCode {
    private Long id;
    private Long visitRequestId;
    private String codeHash;
    private String rawCode; // Código antes de hashear (transitorio)
    private CodeType codeType;
    private AccessCodeStatus status;
    private Integer entriesUsed;
    private Instant validFrom;
    private Instant validUntil;
    private Instant createdAt;
    private Instant updatedAt;
}
