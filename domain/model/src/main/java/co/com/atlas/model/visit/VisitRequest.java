package co.com.atlas.model.visit;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para VisitRequest (Solicitudes de visita).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class VisitRequest {
    private Long id;
    private Long organizationId;
    private Long unitId;
    private Long requestedBy;
    private String visitorName;
    private String visitorDocument;
    private String visitorPhone;
    private String visitorEmail;
    private String vehiclePlate;
    private String purpose;
    private String reason;
    private Instant validFrom;
    private Instant validUntil;
    private RecurrenceType recurrenceType;
    private String recurrenceDays; // JSON como String [1,3,5]
    private Integer maxEntries;
    private VisitStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
