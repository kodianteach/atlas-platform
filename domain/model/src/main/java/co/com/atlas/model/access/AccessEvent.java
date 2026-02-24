package co.com.atlas.model.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para eventos de acceso en portería.
 * Registra entradas (QR/manual) y salidas de vehículos.
 * Soporta operación offline con sincronización posterior.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccessEvent {
    private Long id;
    private Long organizationId;
    private Long authorizationId;
    private Long porterUserId;
    private String deviceId;
    private AccessAction action;
    private ScanResult scanResult;
    private String personName;
    private String personDocument;
    private String vehiclePlate;
    private Boolean vehicleMatch;
    private boolean offlineValidated;
    private String notes;
    private Instant scannedAt;
    private Instant syncedAt;
    private Instant createdAt;
}
