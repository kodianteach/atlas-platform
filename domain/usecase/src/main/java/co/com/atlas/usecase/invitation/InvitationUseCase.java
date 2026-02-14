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
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.role.Role;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.userrolemulti.UserRoleMulti;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final NotificationGateway notificationGateway;
    private final RoleRepository roleRepository;
    private final UserRoleMultiRepository userRoleMultiRepository;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;
    
    private static final int DEFAULT_EXPIRATION_DAYS = 7;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String OWNER_ROLE_CODE = "OWNER";
    
    /**
     * Interface funcional para codificación de contraseña.
     */
    public interface PasswordEncoder {
        String encode(String rawPassword);
    }
    
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
                                        
                                        // Crear usuario PRE_REGISTERED si no existe
                                        return ensureUserExists(invitation.getEmail())
                                            .flatMap(user -> {
                                                String token = UUID.randomUUID().toString();
                                                Instant expiresAt = Instant.now().plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS);
                                                
                                                Invitation newInvitation = invitation.toBuilder()
                                                        .invitationToken(token)
                                                        .status(InvitationStatus.PENDING)
                                                        .invitedBy(invitedByUserId)
                                                        .expiresAt(expiresAt)
                                                        .build();
                                                
                                                return invitationRepository.save(newInvitation)
                                                        .doOnNext(saved -> System.out.println("[InvitationUseCase] Invitation saved with id: " + saved.getId()))
                                                        .flatMap(savedInvitation -> 
                                                            sendInvitationEmail(savedInvitation, org.getName(), invitedByUserId)
                                                                .thenReturn(savedInvitation)
                                                        );
                                            });
                                    }));
                });
    }
    
    /**
     * Envía el correo de invitación.
     */
    private Mono<Void> sendInvitationEmail(Invitation invitation, String organizationName, Long invitedByUserId) {
        System.out.println("[InvitationUseCase] sendInvitationEmail called - email: " + invitation.getEmail() + ", org: " + organizationName + ", invitedBy: " + invitedByUserId);
        
        String invitationUrl = frontendUrl + "/invitation/accept?token=" + invitation.getInvitationToken();
        String expiresAtFormatted = DATE_FORMATTER.format(invitation.getExpiresAt().atZone(ZoneId.systemDefault()));
        
        System.out.println("[InvitationUseCase] Invitation URL: " + invitationUrl);
        
        return authUserRepository.findById(invitedByUserId)
                .doOnNext(user -> System.out.println("[InvitationUseCase] Found user: " + user.getNames()))
                .map(AuthUser::getNames)
                .defaultIfEmpty("Administrador")
                .doOnNext(name -> System.out.println("[InvitationUseCase] Sending invitation email to: " + invitation.getEmail() + ", invitedBy: " + name))
                .flatMap(invitedByName -> {
                    System.out.println("[InvitationUseCase] Calling notificationGateway.sendOrganizationInvitationEmail");
                    return notificationGateway.sendOrganizationInvitationEmail(
                            invitation.getEmail(),
                            organizationName,
                            invitationUrl,
                            invitedByName,
                            expiresAtFormatted
                    );
                })
                .doOnSuccess(v -> System.out.println("[InvitationUseCase] Invitation email sent successfully"))
                .doOnError(e -> System.err.println("[InvitationUseCase] Error sending invitation email: " + e.getMessage()));
    }
    
    /**
     * Acepta una invitación por token (solo token, sin userId).
     * Busca al usuario por el email de la invitación.
     */
    public Mono<Invitation> acceptByToken(String token) {
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
                    
                    // Buscar usuario por email de la invitación
                    return authUserRepository.findByEmail(invitation.getEmail())
                            .switchIfEmpty(Mono.error(new NotFoundException("Usuario no encontrado para email: " + invitation.getEmail())))
                            .flatMap(user -> createMembership(user.getId(), invitation)
                                    .then(Mono.defer(() -> {
                                        Invitation accepted = invitation.toBuilder()
                                                .status(InvitationStatus.ACCEPTED)
                                                .acceptedAt(Instant.now())
                                                .build();
                                        return invitationRepository.save(accepted);
                                    })));
                });
    }
    
    /**
     * Acepta una invitación con datos del propietario.
     * Crea o actualiza el usuario con los datos proporcionados.
     */
    public Mono<Invitation> acceptWithOwnerData(String token, OwnerRegistrationData ownerData) {
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
                    
                    // Crear o actualizar usuario con los datos proporcionados
                    return createOrUpdateOwner(invitation.getEmail(), ownerData)
                            .flatMap(user -> createMembership(user.getId(), invitation)
                                    .then(assignOwnerRole(user, invitation))
                                    .then(Mono.defer(() -> {
                                        Invitation accepted = invitation.toBuilder()
                                                .status(InvitationStatus.ACCEPTED)
                                                .acceptedAt(Instant.now())
                                                .build();
                                        return invitationRepository.save(accepted);
                                    })));
                });
    }
    
    /**
     * Crea o actualiza el usuario propietario con los datos proporcionados.
     * Busca primero por email, luego por documento para evitar duplicados.
     */
    private Mono<AuthUser> createOrUpdateOwner(String email, OwnerRegistrationData data) {
        String hashedPassword = passwordEncoder.encode(data.getPassword());
        
        // Primero buscar por email
        return authUserRepository.findByEmail(email)
                .flatMap(existingUser -> updateExistingUser(existingUser, data, hashedPassword))
                .switchIfEmpty(Mono.defer(() -> {
                    // Si no existe por email, buscar por documento
                    return authUserRepository.findByDocumentTypeAndNumber(
                            data.getDocumentType().name(), data.getDocumentNumber())
                            .flatMap(existingByDoc -> {
                                // Usuario existe con ese documento, actualizar con nuevo email
                                AuthUser updated = existingByDoc.toBuilder()
                                        .email(email)
                                        .names(data.getNames())
                                        .phone(data.getPhone())
                                        .passwordHash(hashedPassword)
                                        .status(UserStatus.ACTIVE)
                                        .active(true)
                                        .build();
                                return authUserRepository.save(updated);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // No existe ni por email ni por documento, crear nuevo
                                AuthUser newUser = AuthUser.builder()
                                        .email(email)
                                        .names(data.getNames())
                                        .phone(data.getPhone())
                                        .documentType(data.getDocumentType())
                                        .documentNumber(data.getDocumentNumber())
                                        .passwordHash(hashedPassword)
                                        .status(UserStatus.ACTIVE)
                                        .active(true)
                                        .build();
                                return authUserRepository.save(newUser);
                            }));
                }));
    }
    
    /**
     * Actualiza un usuario existente con los nuevos datos.
     */
    private Mono<AuthUser> updateExistingUser(AuthUser existingUser, OwnerRegistrationData data, String hashedPassword) {
        AuthUser updated = existingUser.toBuilder()
                .names(data.getNames())
                .phone(data.getPhone())
                .documentType(data.getDocumentType())
                .documentNumber(data.getDocumentNumber())
                .passwordHash(hashedPassword)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();
        return authUserRepository.save(updated);
    }
    
    /**
     * Asigna el rol OWNER al usuario en la organización.
     */
    private Mono<Void> assignOwnerRole(AuthUser user, Invitation invitation) {
        return roleRepository.findByCode(OWNER_ROLE_CODE)
                .switchIfEmpty(Mono.error(new NotFoundException("Role", OWNER_ROLE_CODE)))
                .flatMap(role -> userRoleMultiRepository.existsByUserIdAndOrganizationIdAndRoleId(
                        user.getId(), invitation.getOrganizationId(), role.getId())
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                return Mono.empty();
                            }
                            UserRoleMulti userRole = UserRoleMulti.builder()
                                    .userId(user.getId())
                                    .organizationId(invitation.getOrganizationId())
                                    .roleId(role.getId())
                                    .isPrimary(true)
                                    .assignedAt(Instant.now())
                                    .build();
                            return userRoleMultiRepository.save(userRole).then();
                        }));
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
                    
                    return invitationRepository.save(updated)
                            .flatMap(savedInvitation -> 
                                organizationRepository.findById(savedInvitation.getOrganizationId())
                                    .flatMap(org -> 
                                        sendInvitationEmail(savedInvitation, org.getName(), savedInvitation.getInvitedBy())
                                            .thenReturn(savedInvitation)
                                    )
                            );
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
            
            // Obtener roleId: usar el de la invitación o buscar el rol OWNER
            Mono<Long> roleIdMono;
            if (invitation.getRoleId() != null) {
                roleIdMono = Mono.just(invitation.getRoleId());
            } else {
                roleIdMono = roleRepository.findByCode(OWNER_ROLE_CODE)
                        .map(Role::getId)
                        .switchIfEmpty(Mono.error(new NotFoundException("Role", OWNER_ROLE_CODE)));
            }
            
            return roleIdMono.flatMap(roleId -> {
                UserUnit userUnit = UserUnit.builder()
                        .userId(userId)
                        .unitId(invitation.getUnitId())
                        .roleId(roleId)
                        .ownershipType(ownershipType)
                        .isPrimary(invitation.getType() == InvitationType.UNIT_OWNER)
                        .status("ACTIVE")
                        .invitedBy(invitation.getInvitedBy())
                        .joinedAt(Instant.now())
                        .build();
                
                return orgMembership.then(userUnitRepository.save(userUnit)).then();
            });
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
    
    /**
     * Asegura que existe un usuario para el email dado.
     * Si no existe, crea uno con estado PRE_REGISTERED.
     */
    private Mono<AuthUser> ensureUserExists(String email) {
        return authUserRepository.findByEmail(email)
            .switchIfEmpty(Mono.defer(() -> {
                // Crear usuario PRE_REGISTERED
                AuthUser newUser = AuthUser.builder()
                    .names("Usuario Invitado")
                    .email(email)
                    .status(UserStatus.PRE_REGISTERED)
                    .active(false)
                    .build();
                
                return authUserRepository.save(newUser)
                    .doOnSuccess(u -> System.out.println("[InvitationUseCase] Usuario PRE_REGISTERED creado: id=" + u.getId() + ", email=" + u.getEmail()));
            }));
    }
}
