package co.com.atlas.usecase.invitation;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.userorganization.UserOrganization;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userunit.OwnershipType;
import co.com.atlas.model.userunit.UserUnit;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Caso de uso para gestión de invitaciones.
 */
@RequiredArgsConstructor
public class InvitationUseCase {
    
    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UnitRepository unitRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserUnitRepository userUnitRepository;
    private final AuthUserRepository authUserRepository;
    
    private static final int DEFAULT_EXPIRATION_DAYS = 7;
    
    /**
     * Crea una nueva invitación.
     */
    public Mono<Invitation> create(Invitation invitation, Long invitedByUserId) {
        return organizationRepository.findById(invitation.getOrganizationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Organization", invitation.getOrganizationId())))
                .flatMap(org -> {
                    // Si la invitación es para una unidad, validar que existe
                    Mono<Void> unitValidation = Mono.empty();
                    if (invitation.getUnitId() != null) {
                        unitValidation = unitRepository.findById(invitation.getUnitId())
                                .switchIfEmpty(Mono.error(new NotFoundException("Unit", invitation.getUnitId())))
                                .then();
                    }
                    
                    return unitValidation.then(
                            invitationRepository.existsPendingByEmailAndOrganizationId(
                                            invitation.getEmail(), invitation.getOrganizationId())
                                    .flatMap(exists -> {
                                        if (Boolean.TRUE.equals(exists)) {
                                            return Mono.error(new DuplicateException(
                                                    "Ya existe una invitación pendiente para este email en esta organización"));
                                        }
                                        
                                        String token = UUID.randomUUID().toString();
                                        Instant expiresAt = Instant.now().plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS);
                                        
                                        Invitation newInvitation = invitation.toBuilder()
                                                .invitationToken(token)
                                                .status(InvitationStatus.PENDING)
                                                .invitedBy(invitedByUserId)
                                                .expiresAt(expiresAt)
                                                .build();
                                        
                                        return invitationRepository.save(newInvitation);
                                    }));
                });
    }
    
    /**
     * Acepta una invitación por token.
     */
    public Mono<Invitation> accept(String token, Long userId) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new NotFoundException("Invitación no encontrada")))
                .flatMap(invitation -> {
                    // Validar estado
                    if (invitation.getStatus() != InvitationStatus.PENDING) {
                        return Mono.error(new BusinessException(
                                "Esta invitación ya fue " + invitation.getStatus().name().toLowerCase(),
                                "INVITATION_" + invitation.getStatus().name()));
                    }
                    
                    // Validar expiración
                    if (invitation.getExpiresAt().isBefore(Instant.now())) {
                        Invitation expired = invitation.toBuilder().status(InvitationStatus.EXPIRED).build();
                        return invitationRepository.save(expired)
                                .then(Mono.error(new BusinessException(
                                        "Esta invitación ha expirado", "INVITATION_EXPIRED")));
                    }
                    
                    // Crear membresía en organización
                    return createMembership(userId, invitation)
                            .then(Mono.defer(() -> {
                                Invitation accepted = invitation.toBuilder()
                                        .status(InvitationStatus.ACCEPTED)
                                        .acceptedAt(Instant.now())
                                        .build();
                                return invitationRepository.save(accepted);
                            }));
                });
    }
    
    /**
     * Cancela una invitación.
     */
    public Mono<Void> cancel(Long id) {
        return invitationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Invitation", id)))
                .flatMap(invitation -> {
                    if (invitation.getStatus() != InvitationStatus.PENDING) {
                        return Mono.error(new BusinessException(
                                "Solo se pueden cancelar invitaciones pendientes",
                                "INVALID_STATUS"));
                    }
                    Invitation cancelled = invitation.toBuilder().status(InvitationStatus.CANCELLED).build();
                    return invitationRepository.save(cancelled).then();
                });
    }
    
    /**
     * Reenvía una invitación (genera nuevo token y fecha de expiración).
     */
    public Mono<Invitation> resend(Long id) {
        return invitationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Invitation", id)))
                .flatMap(invitation -> {
                    if (invitation.getStatus() != InvitationStatus.PENDING && 
                        invitation.getStatus() != InvitationStatus.EXPIRED) {
                        return Mono.error(new BusinessException(
                                "Solo se pueden reenviar invitaciones pendientes o expiradas",
                                "INVALID_STATUS"));
                    }
                    
                    String newToken = UUID.randomUUID().toString();
                    Instant newExpiresAt = Instant.now().plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS);
                    
                    Invitation updated = invitation.toBuilder()
                            .invitationToken(newToken)
                            .status(InvitationStatus.PENDING)
                            .expiresAt(newExpiresAt)
                            .build();
                    
                    return invitationRepository.save(updated);
                });
    }
    
    /**
     * Obtiene una invitación por ID.
     */
    public Mono<Invitation> findById(Long id) {
        return invitationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Invitation", id)));
    }
    
    /**
     * Obtiene una invitación por token.
     */
    public Mono<Invitation> findByToken(String token) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new NotFoundException("Invitación no encontrada")));
    }
    
    /**
     * Lista las invitaciones de una organización.
     */
    public Flux<Invitation> findByOrganizationId(Long organizationId) {
        return invitationRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Lista las invitaciones pendientes de una organización.
     */
    public Flux<Invitation> findPendingByOrganizationId(Long organizationId) {
        return invitationRepository.findByOrganizationId(organizationId)
                .filter(inv -> inv.getStatus() == InvitationStatus.PENDING);
    }
    
    private Mono<Void> createMembership(Long userId, Invitation invitation) {
        // Crear user_organizations
        UserOrganization userOrg = UserOrganization.builder()
                .userId(userId)
                .organizationId(invitation.getOrganizationId())
                .status("ACTIVE")
                .joinedAt(Instant.now())
                .build();
        
        Mono<Void> orgMembership = userOrganizationRepository.existsByUserIdAndOrganizationId(userId, invitation.getOrganizationId())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.empty();
                    }
                    return userOrganizationRepository.save(userOrg).then();
                });
        
        // Si es invitación a unidad, crear user_units
        if (invitation.getUnitId() != null) {
            OwnershipType ownershipType = mapInvitationTypeToOwnership(invitation.getType());
            
            UserUnit userUnit = UserUnit.builder()
                    .userId(userId)
                    .unitId(invitation.getUnitId())
                    .roleId(invitation.getRoleId())
                    .ownershipType(ownershipType)
                    .isPrimary(invitation.getType() == InvitationType.UNIT_OWNER)
                    .status("ACTIVE")
                    .invitedBy(invitation.getInvitedBy())
                    .joinedAt(Instant.now())
                    .build();
            
            return orgMembership.then(userUnitRepository.save(userUnit)).then();
        }
        
        return orgMembership;
    }
    
    private OwnershipType mapInvitationTypeToOwnership(InvitationType type) {
        return switch (type) {
            case UNIT_OWNER -> OwnershipType.OWNER;
            case UNIT_TENANT -> OwnershipType.TENANT;
            case UNIT_FAMILY -> OwnershipType.FAMILY;
            default -> OwnershipType.GUEST;
        };
    }
}
