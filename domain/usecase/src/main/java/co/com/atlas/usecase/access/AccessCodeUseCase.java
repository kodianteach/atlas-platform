package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessCode;
import co.com.atlas.model.access.AccessCodeStatus;
import co.com.atlas.model.access.AccessScanLog;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessCodeRepository;
import co.com.atlas.model.access.gateways.AccessScanLogRepository;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.visit.VisitRequest;
import co.com.atlas.model.visit.gateways.VisitRequestRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Caso de uso para validación de códigos de acceso.
 */
@RequiredArgsConstructor
public class AccessCodeUseCase {
    
    private final AccessCodeRepository accessCodeRepository;
    private final AccessScanLogRepository accessScanLogRepository;
    private final VisitRequestRepository visitRequestRepository;
    
    /**
     * Valida un código de acceso.
     *
     * @param rawCode Código sin hashear
     * @param scannedBy ID del usuario que escanea (portero)
     * @param scanLocation Ubicación del escaneo
     * @param deviceInfo Información del dispositivo
     * @return Resultado de la validación
     */
    public Mono<AccessScanLog> validateCode(String rawCode, Long scannedBy, 
                                             String scanLocation, String deviceInfo) {
        String codeHash = hashCode(rawCode);
        
        return accessCodeRepository.findByCodeHash(codeHash)
                .switchIfEmpty(Mono.defer(() -> {
                    // Código no encontrado - registrar intento inválido
                    AccessScanLog log = AccessScanLog.builder()
                            .scannedBy(scannedBy)
                            .scanResult(ScanResult.INVALID)
                            .scanLocation(scanLocation)
                            .deviceInfo(deviceInfo)
                            .notes("Código no encontrado")
                            .createdAt(Instant.now())
                            .build();
                    return accessScanLogRepository.save(log)
                            .then(Mono.error(new NotFoundException("Código de acceso no válido")));
                }))
                .flatMap(accessCode -> validateAndLogScan(accessCode, scannedBy, scanLocation, deviceInfo));
    }
    
    /**
     * Obtiene un código de acceso por ID de visita.
     */
    public Mono<AccessCode> findByVisitRequestId(Long visitRequestId) {
        return accessCodeRepository.findByVisitRequestId(visitRequestId)
                .filter(code -> code.getStatus() == AccessCodeStatus.ACTIVE)
                .next();
    }
    
    /**
     * Obtiene todos los códigos de acceso de una visita.
     */
    public Flux<AccessCode> findAllByVisitRequestId(Long visitRequestId) {
        return accessCodeRepository.findByVisitRequestId(visitRequestId);
    }
    
    /**
     * Revoca un código de acceso.
     */
    public Mono<Void> revoke(Long accessCodeId) {
        return accessCodeRepository.findById(accessCodeId)
                .switchIfEmpty(Mono.error(new NotFoundException("AccessCode", accessCodeId)))
                .flatMap(code -> {
                    AccessCode revoked = code.toBuilder().status(AccessCodeStatus.REVOKED).build();
                    return accessCodeRepository.save(revoked).then();
                });
    }
    
    /**
     * Lista el log de escaneos de un código.
     */
    public Flux<AccessScanLog> getScanLogs(Long accessCodeId) {
        return accessScanLogRepository.findByAccessCodeId(accessCodeId);
    }
    
    private Mono<AccessScanLog> validateAndLogScan(AccessCode code, Long scannedBy,
                                                    String scanLocation, String deviceInfo) {
        Instant now = Instant.now();
        ScanResult result;
        String notes = null;
        boolean incrementEntries = false;
        AccessCodeStatus newStatus = null;
        
        // Verificar estado del código
        if (code.getStatus() == AccessCodeStatus.REVOKED) {
            result = ScanResult.REVOKED;
            notes = "Código revocado";
        } else if (code.getStatus() == AccessCodeStatus.USED) {
            result = ScanResult.ALREADY_USED;
            notes = "Código ya utilizado (límite de entradas alcanzado)";
        } else if (code.getStatus() == AccessCodeStatus.EXPIRED) {
            result = ScanResult.EXPIRED;
            notes = "Código expirado";
        } else if (now.isBefore(code.getValidFrom()) || now.isAfter(code.getValidUntil())) {
            result = ScanResult.EXPIRED;
            notes = "Código fuera del rango de fechas válido";
            newStatus = AccessCodeStatus.EXPIRED;
        } else {
            // Código válido - verificar máximo de entradas
            return visitRequestRepository.findById(code.getVisitRequestId())
                    .flatMap(visitRequest -> {
                        Integer maxEntries = visitRequest.getMaxEntries();
                        int currentEntries = code.getEntriesUsed() != null ? code.getEntriesUsed() : 0;
                        
                        if (maxEntries != null && currentEntries >= maxEntries) {
                            return createScanLog(code.getId(), scannedBy, ScanResult.ALREADY_USED,
                                    scanLocation, deviceInfo, "Límite de entradas alcanzado");
                        }
                        
                        // Código válido - permitir entrada
                        AccessCode updated = code.toBuilder()
                                .entriesUsed(currentEntries + 1)
                                .build();
                        
                        return accessCodeRepository.save(updated)
                                .flatMap(savedCode -> {
                                    // Verificar si alcanzó el límite después de incrementar
                                    if (maxEntries != null && (currentEntries + 1) >= maxEntries) {
                                        AccessCode usedCode = savedCode.toBuilder().status(AccessCodeStatus.USED).build();
                                        return accessCodeRepository.save(usedCode)
                                                .then(createScanLog(code.getId(), scannedBy, ScanResult.VALID,
                                                        scanLocation, deviceInfo, "Entrada válida - código agotado"));
                                    }
                                    return createScanLog(code.getId(), scannedBy, ScanResult.VALID,
                                            scanLocation, deviceInfo, "Entrada válida");
                                });
                    });
        }
        
        // Actualizar estado si es necesario
        Mono<Void> updateStatusMono = Mono.empty();
        if (newStatus != null) {
            final AccessCodeStatus finalStatus = newStatus;
            AccessCode updatedCode = code.toBuilder().status(finalStatus).build();
            updateStatusMono = accessCodeRepository.save(updatedCode).then();
        }
        
        final ScanResult finalResult = result;
        final String finalNotes = notes;
        
        return updateStatusMono.then(createScanLog(code.getId(), scannedBy, finalResult, 
                scanLocation, deviceInfo, finalNotes));
    }
    
    private Mono<AccessScanLog> createScanLog(Long accessCodeId, Long scannedBy, 
                                               ScanResult result, String scanLocation,
                                               String deviceInfo, String notes) {
        AccessScanLog log = AccessScanLog.builder()
                .accessCodeId(accessCodeId)
                .scannedBy(scannedBy)
                .scanResult(result)
                .scanLocation(scanLocation)
                .deviceInfo(deviceInfo)
                .notes(notes)
                .createdAt(Instant.now())
                .build();
        
        return accessScanLogRepository.save(log);
    }
    
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al hashear código", e);
        }
    }
}
