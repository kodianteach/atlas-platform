package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.BusinessException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Caso de uso para validación de autorización por número de documento.
 * Alternativa al scanner QR: el portero ingresa el documento de identidad
 * del visitante y el sistema busca autorizaciones vigentes.
 */
@RequiredArgsConstructor
public class ValidateByDocumentUseCase {

    private static final long MAX_CLOCK_SKEW_MINUTES = 10;

    private final VisitorAuthorizationRepository visitorAuthorizationRepository;
    private final AccessEventRepository accessEventRepository;

    /**
     * Busca autorizaciones activas por documento de identidad.
     * Incluye autorizaciones vigentes y próximas (hasta 24h antes de validFrom)
     * para que el portero pueda visualizarlas. La validación estricta de fechas
     * se aplica solo al registrar el acceso en {@link #validateAndRegister}.
     *
     * @param personDocument Número de documento del visitante
     * @param organizationId ID de la organización
     * @return Autorizaciones activas encontradas (vigentes + próximas)
     */
    public Flux<VisitorAuthorization> findActiveByDocument(String personDocument, Long organizationId) {
        if (personDocument == null || personDocument.isBlank()) {
            return Flux.error(new BusinessException("El número de documento es obligatorio", "DOCUMENT_REQUIRED"));
        }

        Instant now = Instant.now();
        Instant adjustedPast = now.minus(MAX_CLOCK_SKEW_MINUTES, ChronoUnit.MINUTES);

        return visitorAuthorizationRepository.findByOrganizationId(organizationId)
                .filter(auth -> auth.getStatus() == AuthorizationStatus.ACTIVE)
                .filter(auth -> personDocument.equalsIgnoreCase(auth.getPersonDocument()))
                .filter(auth -> !adjustedPast.isAfter(auth.getValidTo()));
    }

    /**
     * Obtiene los eventos de acceso registrados para una organización.
     *
     * @param organizationId ID de la organización
     * @return Eventos de acceso ordenados por fecha
     */
    public Flux<AccessEvent> getAccessEventsByOrganization(Long organizationId) {
        return accessEventRepository.findByOrganizationId(organizationId);
    }

    /**
     * Valida una autorización específica por documento y registra el evento de acceso.
     *
     * @param authorizationId ID de la autorización seleccionada
     * @param porterUserId    ID del portero
     * @param deviceId        ID del dispositivo
     * @param organizationId  ID de la organización
     * @return Evento de acceso registrado
     */
    public Mono<AccessEvent> validateAndRegister(Long authorizationId, Long porterUserId,
                                                  String deviceId, Long organizationId) {
        return visitorAuthorizationRepository.findById(authorizationId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        "Autorización no encontrada", "AUTHORIZATION_NOT_FOUND")))
                .flatMap(auth -> {
                    if (auth.getStatus() != AuthorizationStatus.ACTIVE) {
                        return createAndSaveEvent(organizationId, authorizationId, porterUserId,
                                deviceId, ScanResult.REVOKED, auth.getPersonName(),
                                auth.getPersonDocument(), auth.getVehiclePlate(),
                                "Autorización revocada o inactiva");
                    }

                    Instant now = Instant.now();
                    Instant from = auth.getValidFrom().minus(MAX_CLOCK_SKEW_MINUTES, ChronoUnit.MINUTES);
                    Instant to = auth.getValidTo().plus(MAX_CLOCK_SKEW_MINUTES, ChronoUnit.MINUTES);

                    if (now.isBefore(from) || now.isAfter(to)) {
                        return createAndSaveEvent(organizationId, authorizationId, porterUserId,
                                deviceId, ScanResult.EXPIRED, auth.getPersonName(),
                                auth.getPersonDocument(), auth.getVehiclePlate(),
                                "Autorización fuera de rango de fechas");
                    }

                    return createAndSaveEvent(organizationId, authorizationId, porterUserId,
                            deviceId, ScanResult.VALID, auth.getPersonName(),
                            auth.getPersonDocument(), auth.getVehiclePlate(), null);
                });
    }

    private Mono<AccessEvent> createAndSaveEvent(Long orgId, Long authId, Long porterUserId,
                                                  String deviceId, ScanResult result,
                                                  String personName, String personDocument,
                                                  String vehiclePlate, String notes) {
        AccessEvent event = AccessEvent.builder()
                .organizationId(orgId)
                .authorizationId(authId)
                .porterUserId(porterUserId)
                .deviceId(deviceId)
                .action(AccessAction.ENTRY)
                .scanResult(result)
                .personName(personName)
                .personDocument(personDocument)
                .vehiclePlate(vehiclePlate)
                .offlineValidated(false)
                .notes(notes)
                .scannedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        return accessEventRepository.save(event);
    }
}
