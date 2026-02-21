package co.com.atlas.usecase.invitation;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationFilters;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.invitation.gateways.InvitationAuditRepository;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import co.com.atlas.model.organization.gateways.OrganizationConfigurationRepository;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.permission.gateways.PermissionRepository;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.userorganization.UserOrganization;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userunit.OwnershipType;
import co.com.atlas.model.userunit.UserUnit;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import co.com.atlas.model.userunitpermission.UserUnitPermission;
import co.com.atlas.model.userunitpermission.gateways.UserUnitPermissionRepository;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private final InvitationAuditRepository invitationAuditRepository;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;
    private final PermissionRepository permissionRepository;
    private final UserUnitPermissionRepository userUnitPermissionRepository;
    private final String frontendUrl;
    
    private static final int DEFAULT_EXPIRATION_DAYS = 7;
    private static final int SELF_REGISTER_EXPIRATION_HOURS = 24;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String OWNER_ROLE_CODE = "OWNER";
    private static final String RESIDENT_ROLE_CODE = "RESIDENT";
    
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
            case OWNER_SELF_REGISTER -> OwnershipType.OWNER;
            case RESIDENT_INVITE -> OwnershipType.TENANT;
            default -> OwnershipType.GUEST;
        };
    }
    
    // ==================== HU #9 — Owner Self-Registration & Resident Invitation ====================
    
    /**
     * Creates an invitation for owner self-registration.
     * Generated by ADMIN_ATLAS. Token is generic (no email), 24h expiration, single-use.
     *
     * @param organizationId the organization ID
     * @param invitedByUserId the admin user who creates the invitation
     * @return the created invitation with URL
     */
    public Mono<Invitation> createOwnerInvitation(Long organizationId, Long invitedByUserId) {
        return organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Organization", organizationId)))
                .flatMap(org -> {
                    String token = UUID.randomUUID().toString();
                    Instant expiresAt = Instant.now().plus(SELF_REGISTER_EXPIRATION_HOURS, ChronoUnit.HOURS);
                    
                    Invitation invitation = Invitation.builder()
                            .organizationId(organizationId)
                            .invitationToken(token)
                            .type(InvitationType.OWNER_SELF_REGISTER)
                            .status(InvitationStatus.PENDING)
                            .invitedBy(invitedByUserId)
                            .expiresAt(expiresAt)
                            .metadata("{\"invitationUrl\": \"" + frontendUrl + "/register?token=" + token + "\"}")
                            .build();
                    
                    return invitationRepository.save(invitation)
                            .flatMap(saved -> invitationAuditRepository.logAction(
                                    saved.getId(), InvitationAuditRepository.ACTION_CREATED, invitedByUserId)
                                    .thenReturn(saved));
                });
    }
    
    /**
     * Creates an invitation for a resident of a specific unit.
     * Generated by OWNER. Token includes unitId and optional permissions.
     * If unitId is null, the owner's primary unit is resolved automatically.
     *
     * @param organizationId the organization ID
     * @param unitId the unit ID (owner's unit), may be null to auto-resolve
     * @param invitedByUserId the owner user who creates the invitation
     * @param permissionsJson optional JSON with selected permissions (null if inheriting)
     * @return the created invitation with URL
     */
    public Mono<Invitation> createResidentInvitation(Long organizationId, Long unitId,
                                                      Long invitedByUserId, String permissionsJson) {
        // If unitId not provided, resolve the owner's primary unit
        Mono<Long> resolvedUnitId;
        if (unitId != null) {
            resolvedUnitId = Mono.just(unitId);
        } else {
            resolvedUnitId = userUnitRepository.findPrimaryByUserId(invitedByUserId)
                    .map(uu -> uu.getUnitId())
                    .switchIfEmpty(
                        userUnitRepository.findByUserId(invitedByUserId)
                            .next()
                            .map(uu -> uu.getUnitId())
                    )
                    .switchIfEmpty(Mono.error(new BusinessException(
                            "No se encontró una unidad asociada al propietario", "OWNER_NO_UNIT")));
        }

        return resolvedUnitId.flatMap(resolvedId ->
            organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Organization", organizationId)))
                .flatMap(org -> unitRepository.findById(resolvedId)
                        .switchIfEmpty(Mono.error(new NotFoundException("Unit", resolvedId)))
                        .flatMap(unit -> {
                            String token = UUID.randomUUID().toString();
                            Instant expiresAt = Instant.now().plus(SELF_REGISTER_EXPIRATION_HOURS, ChronoUnit.HOURS);
                            
                            Invitation invitation = Invitation.builder()
                                    .organizationId(organizationId)
                                    .unitId(resolvedId)
                                    .invitationToken(token)
                                    .type(InvitationType.RESIDENT_INVITE)
                                    .status(InvitationStatus.PENDING)
                                    .invitedBy(invitedByUserId)
                                    .initialPermissions(permissionsJson)
                                    .expiresAt(expiresAt)
                                    .metadata("{\"invitationUrl\": \"" + frontendUrl + "/register?token=" + token + "\"}")
                                    .build();
                            
                            return invitationRepository.save(invitation)
                                    .flatMap(saved -> invitationAuditRepository.logAction(
                                            saved.getId(), InvitationAuditRepository.ACTION_CREATED, invitedByUserId)
                                            .thenReturn(saved));
                        }))
        );
    }
    
    /**
     * Accepts an owner self-registration invitation.
     * Creates user with ACTIVE status and OWNER role, associates to selected unit.
     *
     * @param token the invitation token
     * @param ownerData the registration data from the owner
     * @param unitId the unit selected by the owner via autocomplete
     * @return the updated invitation
     */
    public Mono<Invitation> acceptOwnerSelfRegister(String token, OwnerRegistrationData ownerData, Long unitId) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new NotFoundException("Invitación no encontrada")))
                .flatMap(invitation -> {
                    // Validate type
                    if (invitation.getType() != InvitationType.OWNER_SELF_REGISTER) {
                        return Mono.error(new BusinessException(
                                "Token no es de tipo registro de propietario", "INVALID_TOKEN_TYPE"));
                    }
                    
                    // Validate status
                    if (invitation.getStatus() != InvitationStatus.PENDING) {
                        String errorCode = "INVITATION_" + invitation.getStatus().name();
                        return Mono.error(new BusinessException(
                                "Este enlace de invitación ya fue utilizado", errorCode));
                    }
                    
                    // Validate expiration
                    if (invitation.getExpiresAt().isBefore(Instant.now())) {
                        Invitation expired = invitation.toBuilder().status(InvitationStatus.EXPIRED).build();
                        return invitationRepository.save(expired)
                                .then(Mono.error(new BusinessException(
                                        "El enlace de invitación ha expirado", "INVITATION_EXPIRED")));
                    }
                    
                    // Validate password strength
                    if (!isPasswordStrong(ownerData.getPassword())) {
                        return Mono.error(new BusinessException(
                                "Debe contener mayúscula, minúscula y número (mínimo 8 caracteres)",
                                "WEAK_PASSWORD"));
                    }
                    
                    // Validate unit belongs to organization
                    return unitRepository.findById(unitId)
                            .switchIfEmpty(Mono.error(new NotFoundException("Unit", unitId)))
                            .flatMap(unit -> {
                                if (!unit.getOrganizationId().equals(invitation.getOrganizationId())) {
                                    return Mono.error(new BusinessException(
                                            "La unidad no pertenece a esta organización", "INVALID_UNIT"));
                                }
                                
                                return createOrUpdateUser(ownerData, ownerData.getEmail())
                                        .flatMap(user -> {
                                            // Create membership
                                            UserOrganization userOrg = UserOrganization.builder()
                                                    .userId(user.getId())
                                                    .organizationId(invitation.getOrganizationId())
                                                    .status("ACTIVE")
                                                    .joinedAt(Instant.now())
                                                    .build();
                                            
                                            return userOrganizationRepository.existsByUserIdAndOrganizationId(
                                                            user.getId(), invitation.getOrganizationId())
                                                    .flatMap(exists -> {
                                                        Mono<Void> orgMembership = Boolean.TRUE.equals(exists)
                                                                ? Mono.empty()
                                                                : userOrganizationRepository.save(userOrg).then();
                                                        
                                                        return orgMembership
                                                                .then(assignRole(user, invitation.getOrganizationId(), OWNER_ROLE_CODE))
                                                                .then(createUserUnit(user.getId(), unitId, OWNER_ROLE_CODE,
                                                                        OwnershipType.OWNER, invitation.getInvitedBy()))
                                                                .then(Mono.defer(() -> {
                                                                    Invitation accepted = invitation.toBuilder()
                                                                            .status(InvitationStatus.ACCEPTED)
                                                                            .acceptedAt(Instant.now())
                                                                            .unitId(unitId)
                                                                            .build();
                                                                    return invitationRepository.save(accepted);
                                                                }))
                                                                .flatMap(saved -> invitationAuditRepository.logAction(
                                                                        saved.getId(), InvitationAuditRepository.ACTION_ACCEPTED,
                                                                        user.getId())
                                                                        .thenReturn(saved));
                                                    });
                                        });
                            });
                });
    }
    
    /**
     * Accepts a resident registration invitation.
     * Creates user with ACTIVE status and RESIDENT role. Unit and permissions come from token.
     *
     * @param token the invitation token
     * @param residentData the registration data from the resident
     * @return the updated invitation
     */
    public Mono<Invitation> acceptResidentRegister(String token, ResidentRegistrationData residentData) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new NotFoundException("Invitación no encontrada")))
                .flatMap(invitation -> {
                    // Validate type
                    if (invitation.getType() != InvitationType.RESIDENT_INVITE) {
                        return Mono.error(new BusinessException(
                                "Token no es de tipo invitación de residente", "INVALID_TOKEN_TYPE"));
                    }
                    
                    // Validate status
                    if (invitation.getStatus() != InvitationStatus.PENDING) {
                        String errorCode = "INVITATION_" + invitation.getStatus().name();
                        return Mono.error(new BusinessException(
                                "Este enlace de invitación ya fue utilizado", errorCode));
                    }
                    
                    // Validate expiration
                    if (invitation.getExpiresAt().isBefore(Instant.now())) {
                        Invitation expired = invitation.toBuilder().status(InvitationStatus.EXPIRED).build();
                        return invitationRepository.save(expired)
                                .then(Mono.error(new BusinessException(
                                        "El enlace de invitación ha expirado", "INVITATION_EXPIRED")));
                    }
                    
                    // Validate passwords match
                    if (residentData.getConfirmPassword() != null 
                            && !residentData.getPassword().equals(residentData.getConfirmPassword())) {
                        return Mono.error(new BusinessException(
                                "Las contraseñas no coinciden", "PASSWORDS_MISMATCH"));
                    }
                    
                    // Validate password strength
                    if (!isPasswordStrong(residentData.getPassword())) {
                        return Mono.error(new BusinessException(
                                "Debe contener mayúscula, minúscula y número (mínimo 8 caracteres)",
                                "WEAK_PASSWORD"));
                    }
                    
                    OwnerRegistrationData ownerData = OwnerRegistrationData.builder()
                            .names(residentData.getNames())
                            .phone(residentData.getPhone())
                            .documentType(residentData.getDocumentType())
                            .documentNumber(residentData.getDocumentNumber())
                            .password(residentData.getPassword())
                            .build();
                    
                    return createOrUpdateUser(ownerData, null)
                            .flatMap(user -> {
                                UserOrganization userOrg = UserOrganization.builder()
                                        .userId(user.getId())
                                        .organizationId(invitation.getOrganizationId())
                                        .status("ACTIVE")
                                        .joinedAt(Instant.now())
                                        .build();
                                
                                return userOrganizationRepository.existsByUserIdAndOrganizationId(
                                                user.getId(), invitation.getOrganizationId())
                                        .flatMap(exists -> {
                                            Mono<Void> orgMembership = Boolean.TRUE.equals(exists)
                                                    ? Mono.empty()
                                                    : userOrganizationRepository.save(userOrg).then();
                                            
                                            return orgMembership
                                                    .then(assignRole(user, invitation.getOrganizationId(), RESIDENT_ROLE_CODE))
                                                    .then(createUserUnit(user.getId(), invitation.getUnitId(),
                                                            RESIDENT_ROLE_CODE, OwnershipType.TENANT,
                                                            invitation.getInvitedBy()))
                                                    .flatMap(savedUserUnit -> assignPermissionsToUserUnit(
                                                            savedUserUnit.getId(),
                                                            invitation.getInitialPermissions(),
                                                            invitation.getInvitedBy()))
                                                    .then(Mono.defer(() -> {
                                                        Invitation accepted = invitation.toBuilder()
                                                                .status(InvitationStatus.ACCEPTED)
                                                                .acceptedAt(Instant.now())
                                                                .build();
                                                        return invitationRepository.save(accepted);
                                                    }))
                                                    .flatMap(saved -> invitationAuditRepository.logAction(
                                                            saved.getId(), InvitationAuditRepository.ACTION_ACCEPTED,
                                                            user.getId())
                                                            .thenReturn(saved));
                                        });
                            });
                });
    }
    
    /**
     * Gets invitation history with role-based data scoping.
     * ADMIN_ATLAS sees organization-level data, OWNER/RESIDENT sees unit-level data.
     * @param organizationId the organization ID
     * @param unitId the unit ID (nullable, used for non-admin roles)
     * @param filters the filter criteria
     * @param isAdmin whether the requester has ADMIN_ATLAS role
     * @return Flux of matching invitations
     */
    public Flux<Invitation> getInvitationHistory(Long organizationId, Long unitId, 
            InvitationFilters filters, boolean isAdmin) {
        if (isAdmin) {
            return invitationRepository.findByOrganizationIdWithFilters(organizationId, filters);
        }
        if (unitId != null) {
            return invitationRepository.findByUnitIdWithFilters(unitId, filters);
        }
        return Flux.empty();
    }
    
    /**
     * Searches invitations by organization with filters. Used by ADMIN_ATLAS.
     */
    public Flux<Invitation> findByOrganizationIdWithFilters(Long organizationId, InvitationFilters filters) {
        return invitationRepository.findByOrganizationIdWithFilters(organizationId, filters);
    }
    
    /**
     * Searches invitations by unit with filters. Used by OWNER/RESIDENT.
     */
    public Flux<Invitation> findByUnitIdWithFilters(Long unitId, InvitationFilters filters) {
        return invitationRepository.findByUnitIdWithFilters(unitId, filters);
    }
    
    /**
     * Creates or updates a user based on document number lookup.
     * Used for both owner and resident self-registration.
     */
    private Mono<AuthUser> createOrUpdateUser(OwnerRegistrationData data, String email) {
        String hashedPassword = passwordEncoder.encode(data.getPassword());
        
        // First try by document
        return authUserRepository.findByDocumentTypeAndNumber(
                        data.getDocumentType().name(), data.getDocumentNumber())
                .flatMap(existing -> {
                    AuthUser updated = existing.toBuilder()
                            .names(data.getNames())
                            .phone(data.getPhone())
                            .documentType(data.getDocumentType())
                            .documentNumber(data.getDocumentNumber())
                            .passwordHash(hashedPassword)
                            .status(UserStatus.ACTIVE)
                            .active(true)
                            .build();
                    if (email != null) {
                        updated = updated.toBuilder().email(email).build();
                    }
                    return authUserRepository.save(updated);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new user
                    AuthUser newUser = AuthUser.builder()
                            .names(data.getNames())
                            .phone(data.getPhone())
                            .documentType(data.getDocumentType())
                            .documentNumber(data.getDocumentNumber())
                            .passwordHash(hashedPassword)
                            .email(email)
                            .status(UserStatus.ACTIVE)
                            .active(true)
                            .build();
                    return authUserRepository.save(newUser);
                }));
    }
    
    /**
     * Assigns a role to a user in an organization if not already assigned.
     */
    private Mono<Void> assignRole(AuthUser user, Long organizationId, String roleCode) {
        return roleRepository.findByCode(roleCode)
                .switchIfEmpty(Mono.error(new NotFoundException("Role", roleCode)))
                .flatMap(role -> userRoleMultiRepository.existsByUserIdAndOrganizationIdAndRoleId(
                        user.getId(), organizationId, role.getId())
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                return Mono.empty();
                            }
                            UserRoleMulti userRole = UserRoleMulti.builder()
                                    .userId(user.getId())
                                    .organizationId(organizationId)
                                    .roleId(role.getId())
                                    .isPrimary(true)
                                    .assignedAt(Instant.now())
                                    .build();
                            return userRoleMultiRepository.save(userRole).then();
                        }));
    }
    
    /**
     * Creates a user-unit association if not already existing.
     * Returns the saved UserUnit so callers can use its ID (e.g., to assign permissions).
     */
    private Mono<UserUnit> createUserUnit(Long userId, Long unitId, String roleCode,
                                           OwnershipType ownershipType, Long invitedBy) {
        return roleRepository.findByCode(roleCode)
                .switchIfEmpty(Mono.error(new NotFoundException("Role", roleCode)))
                .flatMap(role -> {
                    UserUnit userUnit = UserUnit.builder()
                            .userId(userId)
                            .unitId(unitId)
                            .roleId(role.getId())
                            .ownershipType(ownershipType)
                            .isPrimary(true)
                            .status("ACTIVE")
                            .invitedBy(invitedBy)
                            .joinedAt(Instant.now())
                            .build();
                    return userUnitRepository.save(userUnit);
                });
    }

    /**
     * Assigns permissions to a UserUnit based on the initialPermissions JSON from the invitation.
     * The JSON format is: {"VISITS_CREATE": true, "VISITS_READ": true, "VISITS_DELETE": false}
     * Only permissions with value=true are assigned.
     *
     * @param userUnitId the ID of the UserUnit to assign permissions to
     * @param initialPermissionsJson the JSON string with permission codes and their enabled state
     * @param grantedBy the user ID of the person who granted the permissions (the owner)
     */
    private Mono<Void> assignPermissionsToUserUnit(Long userUnitId, String initialPermissionsJson, Long grantedBy) {
        if (initialPermissionsJson == null || initialPermissionsJson.isBlank()) {
            return Mono.empty();
        }

        Map<String, Boolean> permissionsMap = parsePermissionsJson(initialPermissionsJson);

        if (permissionsMap.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(permissionsMap.entrySet())
                .filter(Map.Entry::getValue) // only permissions set to true
                .flatMap(entry -> permissionRepository.findByCode(entry.getKey())
                        .flatMap(permission -> {
                            UserUnitPermission uup = UserUnitPermission.builder()
                                    .userUnitId(userUnitId)
                                    .permissionId(permission.getId())
                                    .grantedBy(grantedBy)
                                    .grantedAt(Instant.now())
                                    .build();
                            return userUnitPermissionRepository.save(uup);
                        }))
                .then();
    }

    /**
     * Parses a simple JSON object of the form {"KEY": true, "KEY2": false}
     * into a Map without requiring Jackson in the domain layer.
     */
    private Map<String, Boolean> parsePermissionsJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);
        content = content.trim();
        if (content.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> result = new HashMap<>();
        for (String pair : content.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                boolean value = Boolean.parseBoolean(kv[1].trim());
                result.put(key, value);
            }
        }
        return result;
    }
    
    /**
     * Validates password strength: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit.
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasUpper && hasLower && hasDigit;
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
