package co.com.atlas.r2dbc.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad R2DBC para la tabla access_events.
 * Registra eventos de entrada/salida en portería.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("access_events")
public class AccessEventEntity {

    @Id
    private Long id;

    @Column("organization_id")
    private Long organizationId;

    @Column("authorization_id")
    private Long authorizationId;

    @Column("porter_user_id")
    private Long porterUserId;

    @Column("device_id")
    private String deviceId;

    @Column("action")
    private String action;

    @Column("scan_result")
    private String scanResult;

    @Column("person_name")
    private String personName;

    @Column("person_document")
    private String personDocument;

    @Column("vehicle_plate")
    private String vehiclePlate;

    @Column("vehicle_match")
    private Boolean vehicleMatch;

    @Column("offline_validated")
    private boolean offlineValidated;

    @Column("notes")
    private String notes;

    @Column("scanned_at")
    private Instant scannedAt;

    @Column("synced_at")
    private Instant syncedAt;

    @Column("created_at")
    private Instant createdAt;
}
