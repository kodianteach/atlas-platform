package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.common.BusinessException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Caso de uso para registro de salida de vehículo.
 * El portero registra la placa y datos de la persona que retira el vehículo.
 */
@RequiredArgsConstructor
public class RegisterVehicleExitUseCase {

    private final AccessEventRepository accessEventRepository;

    /**
     * Registra la salida de un vehículo.
     *
     * @param vehiclePlate   Placa del vehículo
     * @param personName     Nombre de la persona que retira
     * @param porterUserId   ID del portero
     * @param organizationId ID de la organización
     * @param deviceId       ID del dispositivo
     * @return Evento de salida registrado
     */
    public Mono<AccessEvent> execute(String vehiclePlate, String personName, Long porterUserId,
                                      Long organizationId, String deviceId) {
        if (vehiclePlate == null || vehiclePlate.isBlank()) {
            return Mono.error(new BusinessException("La placa del vehículo es obligatoria", "VEHICLE_PLATE_REQUIRED"));
        }
        if (personName == null || personName.isBlank()) {
            return Mono.error(new BusinessException("El nombre de la persona es obligatorio", "PERSON_NAME_REQUIRED"));
        }

        AccessEvent event = AccessEvent.builder()
                .organizationId(organizationId)
                .porterUserId(porterUserId)
                .deviceId(deviceId)
                .action(AccessAction.EXIT)
                .scanResult(ScanResult.VALID)
                .personName(personName)
                .vehiclePlate(vehiclePlate.toUpperCase().trim())
                .offlineValidated(false)
                .scannedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        return accessEventRepository.save(event);
    }
}
