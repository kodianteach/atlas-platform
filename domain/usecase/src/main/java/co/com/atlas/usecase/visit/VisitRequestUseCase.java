package co.com.atlas.usecase.visit;

import co.com.atlas.model.access.AccessCode;
import co.com.atlas.model.access.AccessCodeStatus;
import co.com.atlas.model.access.CodeType;
import co.com.atlas.model.access.gateways.AccessCodeRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import co.com.atlas.model.visit.ApprovalAction;
import co.com.atlas.model.visit.VisitApproval;
import co.com.atlas.model.visit.VisitRequest;
import co.com.atlas.model.visit.VisitStatus;
import co.com.atlas.model.visit.gateways.VisitApprovalRepository;
import co.com.atlas.model.visit.gateways.VisitRequestRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Caso de uso para gestión de solicitudes de visita.
 */
@RequiredArgsConstructor
public class VisitRequestUseCase {
    
    private final VisitRequestRepository visitRequestRepository;
    private final VisitApprovalRepository visitApprovalRepository;
    private final AccessCodeRepository accessCodeRepository;
    private final UnitRepository unitRepository;
    private final UserUnitRepository userUnitRepository;
    
    /**
     * Crea una nueva solicitud de visita.
     */
    public Mono<VisitRequest> create(VisitRequest request, Long requestedByUserId) {
        return unitRepository.findById(request.getUnitId())
                .switchIfEmpty(Mono.error(new NotFoundException("Unit", request.getUnitId())))
                .flatMap(unit -> {
                    // Validar que el usuario tiene permisos en la unidad
                    return userUnitRepository.existsByUserIdAndUnitId(requestedByUserId, request.getUnitId())
                            .flatMap(exists -> {
                                if (Boolean.FALSE.equals(exists)) {
                                    return Mono.error(new BusinessException(
                                            "No tienes permisos para crear visitas en esta unidad",
                                            "NO_PERMISSION", 403));
                                }
                                
                                // Validar fechas
                                if (request.getValidFrom().isAfter(request.getValidUntil())) {
                                    return Mono.error(new BusinessException(
                                            "La fecha de inicio debe ser anterior a la fecha de fin",
                                            "INVALID_DATE_RANGE"));
                                }
                                
                                VisitRequest newRequest = request.toBuilder()
                                        .requestedBy(requestedByUserId)
                                        .organizationId(unit.getOrganizationId())
                                        .status(VisitStatus.PENDING)
                                        .build();
                                
                                return visitRequestRepository.save(newRequest);
                            });
                });
    }
    
    /**
     * Aprueba una solicitud de visita.
     */
    public Mono<VisitRequest> approve(Long visitRequestId, Long approvedByUserId) {
        return visitRequestRepository.findById(visitRequestId)
                .switchIfEmpty(Mono.error(new NotFoundException("VisitRequest", visitRequestId)))
                .flatMap(request -> {
                    if (request.getStatus() != VisitStatus.PENDING) {
                        return Mono.error(new BusinessException(
                                "Solo se pueden aprobar solicitudes pendientes",
                                "INVALID_STATUS"));
                    }
                    
                    // Verificar que el usuario tiene permisos (propietario o admin)
                    return userUnitRepository.findByUnitId(request.getUnitId())
                            .filter(uu -> Boolean.TRUE.equals(uu.getIsPrimary()))
                            .next()
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    "No se encontró propietario primario para la unidad",
                                    "NO_PRIMARY_OWNER")))
                            .flatMap(owner -> {
                                if (!owner.getUserId().equals(approvedByUserId)) {
                                    return Mono.error(new BusinessException(
                                            "Solo el propietario puede aprobar visitas",
                                            "NO_PERMISSION", 403));
                                }
                                
                                // Crear registro de aprobación
                                VisitApproval approval = VisitApproval.builder()
                                        .visitRequestId(visitRequestId)
                                        .approvedBy(approvedByUserId)
                                        .action(ApprovalAction.APPROVED)
                                        .createdAt(Instant.now())
                                        .build();
                                
                                return visitApprovalRepository.save(approval)
                                        .then(generateAccessCode(request))
                                        .then(Mono.defer(() -> {
                                            VisitRequest approved = request.toBuilder()
                                                    .status(VisitStatus.APPROVED)
                                                    .build();
                                            return visitRequestRepository.save(approved);
                                        }));
                            });
                });
    }
    
    /**
     * Rechaza una solicitud de visita.
     */
    public Mono<VisitRequest> reject(Long visitRequestId, Long rejectedByUserId, String reason) {
        return visitRequestRepository.findById(visitRequestId)
                .switchIfEmpty(Mono.error(new NotFoundException("VisitRequest", visitRequestId)))
                .flatMap(request -> {
                    if (request.getStatus() != VisitStatus.PENDING) {
                        return Mono.error(new BusinessException(
                                "Solo se pueden rechazar solicitudes pendientes",
                                "INVALID_STATUS"));
                    }
                    
                    VisitApproval approval = VisitApproval.builder()
                            .visitRequestId(visitRequestId)
                            .approvedBy(rejectedByUserId)
                            .action(ApprovalAction.REJECTED)
                            .reason(reason)
                            .createdAt(Instant.now())
                            .build();
                    
                    return visitApprovalRepository.save(approval)
                            .then(Mono.defer(() -> {
                                VisitRequest rejected = request.toBuilder()
                                        .status(VisitStatus.REJECTED)
                                        .build();
                                return visitRequestRepository.save(rejected);
                            }));
                });
    }
    
    /**
     * Cancela una solicitud de visita.
     */
    public Mono<Void> cancel(Long visitRequestId, Long userId) {
        return visitRequestRepository.findById(visitRequestId)
                .switchIfEmpty(Mono.error(new NotFoundException("VisitRequest", visitRequestId)))
                .flatMap(request -> {
                    if (!request.getRequestedBy().equals(userId)) {
                        return Mono.error(new BusinessException(
                                "Solo el solicitante puede cancelar la visita",
                                "NO_PERMISSION", 403));
                    }
                    
                    if (request.getStatus() == VisitStatus.EXPIRED || 
                        request.getStatus() == VisitStatus.CANCELLED) {
                        return Mono.error(new BusinessException(
                                "Esta solicitud ya está cancelada o expirada",
                                "INVALID_STATUS"));
                    }
                    
                    // Si está aprobada, revocar el código de acceso
                    Mono<Void> revokeCode = Mono.empty();
                    if (request.getStatus() == VisitStatus.APPROVED) {
                        revokeCode = accessCodeRepository.findByVisitRequestId(visitRequestId)
                                .filter(code -> code.getStatus() == AccessCodeStatus.ACTIVE)
                                .next()
                                .flatMap(code -> {
                                    AccessCode revoked = code.toBuilder().status(AccessCodeStatus.REVOKED).build();
                                    return accessCodeRepository.save(revoked).then();
                                });
                    }
                    
                    VisitRequest cancelled = request.toBuilder().status(VisitStatus.CANCELLED).build();
                    return revokeCode.then(visitRequestRepository.save(cancelled).then());
                });
    }
    
    /**
     * Obtiene una solicitud por ID.
     */
    public Mono<VisitRequest> findById(Long id) {
        return visitRequestRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("VisitRequest", id)));
    }
    
    /**
     * Lista las solicitudes de una unidad.
     */
    public Flux<VisitRequest> findByUnitId(Long unitId) {
        return visitRequestRepository.findByUnitId(unitId);
    }
    
    /**
     * Lista las solicitudes pendientes de una unidad.
     */
    public Flux<VisitRequest> findPendingByUnitId(Long unitId) {
        return visitRequestRepository.findByUnitId(unitId)
                .filter(vr -> vr.getStatus() == VisitStatus.PENDING);
    }
    
    /**
     * Lista las solicitudes de una organización.
     */
    public Flux<VisitRequest> findByOrganizationId(Long organizationId) {
        return visitRequestRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Lista las solicitudes pendientes de una organización.
     */
    public Flux<VisitRequest> findPendingByOrganizationId(Long organizationId) {
        return visitRequestRepository.findByOrganizationId(organizationId)
                .filter(vr -> vr.getStatus() == VisitStatus.PENDING);
    }
    
    /**
     * Lista las solicitudes creadas por un usuario.
     */
    public Flux<VisitRequest> findByRequestedBy(Long userId) {
        return visitRequestRepository.findByRequestedBy(userId);
    }
    
    private Mono<AccessCode> generateAccessCode(VisitRequest request) {
        String rawCode = UUID.randomUUID().toString();
        String codeHash = hashCode(rawCode);
        
        AccessCode code = AccessCode.builder()
                .visitRequestId(request.getId())
                .codeHash(codeHash)
                .rawCode(rawCode)
                .codeType(CodeType.QR)
                .status(AccessCodeStatus.ACTIVE)
                .entriesUsed(0)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .build();
        
        return accessCodeRepository.save(code);
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
