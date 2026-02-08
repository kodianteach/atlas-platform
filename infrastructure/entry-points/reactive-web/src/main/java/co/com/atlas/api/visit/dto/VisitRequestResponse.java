package co.com.atlas.api.visit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para VisitRequest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitRequestResponse {
    private Long id;
    private Long organizationId;
    private Long unitId;
    private Long requestedBy;
    private String visitorName;
    private String visitorDocument;
    private String visitorPhone;
    private String visitorEmail;
    private String reason;
    private Instant validFrom;
    private Instant validUntil;
    private String recurrenceType;
    private Integer maxEntries;
    private String status;
    private String accessCode;
    private Instant createdAt;
}
