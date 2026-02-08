package co.com.atlas.api.visit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO para crear solicitud de visita.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitRequestDto {
    private Long organizationId;
    private Long unitId;
    private String visitorName;
    private String visitorDocument;
    private String visitorPhone;
    private String visitorEmail;
    private String reason;
    private Instant validFrom;
    private Instant validUntil;
    private String recurrenceType;
    private Integer maxEntries;
}
