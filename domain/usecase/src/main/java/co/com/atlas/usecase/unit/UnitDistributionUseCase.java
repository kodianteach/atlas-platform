package co.com.atlas.usecase.unit;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.auth.validation.UserIdentificationValidator;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.invitation.gateways.InvitationAuditRepository;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.organization.OrganizationConfiguration;
import co.com.atlas.model.organization.gateways.OrganizationConfigurationRepository;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.unit.OwnerInfo;
import co.com.atlas.model.unit.RejectedUnit;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.UnitDistribution;
import co.com.atlas.model.unit.UnitDistributionResult;
import co.com.atlas.model.unit.UnitStatus;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.unit.validation.UnitDistributionValidator;
import co.com.atlas.model.userorganization.UserOrganization;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userunit.OwnershipType;
import co.com.atlas.model.userunit.UserUnit;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Caso de uso para creación de unidades por distribución (rango).
 * 
 * Permite crear múltiples unidades con un prefijo y rango numérico.
 * Ejemplo: code="B", min=1, max=3 genera B-1, B-2, B-3
 * 
 * Opcionalmente puede:
 * - Asignar un propietario a cada unidad
 * - Enviar invitaciones automáticamente
 */
@RequiredArgsConstructor
public class UnitDistributionUseCase {
    
    private static final System.Logger LOGGER = System.getLogger(UnitDistributionUseCase.class.getName());
    
    private final UnitRepository unitRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;
    private final AuthUserRepository authUserRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationAuditRepository invitationAuditRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserUnitRepository userUnitRepository;
    private final RoleRepository roleRepository;
    private final NotificationGateway notificationGateway;
    private final String frontendUrl;
    
    private static final int DEFAULT_EXPIRATION_DAYS = 7;
    private static final String OWNER_ROLE_CODE = "OWNER";
    
    /**
     * Crea unidades basadas en una distribución (rango).
     * Soporta creación parcial: si algunas unidades ya existen, crea solo las nuevas
     * y retorna un resultado con conteos de creadas y rechazadas.
     * 
     * @param distribution parámetros de distribución
     * @param createdBy ID del usuario que crea las unidades
     * @return resultado con unidades creadas y rechazadas
     */
    public Mono<UnitDistributionResult> createByDistribution(UnitDistribution distribution, Long createdBy) {
        // Validar la distribución completa
        UnitDistributionValidator.validateComplete(distribution);
        
        LOGGER.log(System.Logger.Level.INFO, 
            "Iniciando creación de unidades por distribución: org={0}, código={1}, rango={2}-{3}",
            distribution.getOrganizationId(), distribution.getCode(), 
            distribution.getMin(), distribution.getMax());
        
        return organizationRepository.findById(distribution.getOrganizationId())
            .switchIfEmpty(Mono.error(new NotFoundException("Organization", distribution.getOrganizationId())))
            .flatMap(org -> {
                // Validar que el tipo de unidad está permitido
                if (!org.allowsUnitType(distribution.getType().name())) {
                    return Mono.error(new BusinessException(
                        "El tipo de unidad " + distribution.getType().name() + " no está permitido en esta organización",
                        "INVALID_UNIT_TYPE"
                    ));
                }
                
                // Generar códigos a crear
                List<String> codesToCreate = generateCodes(distribution);
                
                // Obtener límite máximo desde la tabla de configuración de organización
                return organizationConfigurationRepository.findByOrganizationId(distribution.getOrganizationId())
                    .map(OrganizationConfiguration::getMaxUnitsPerDistributionOrDefault)
                    .defaultIfEmpty(OrganizationConfiguration.DEFAULT_MAX_UNITS_PER_DISTRIBUTION)
                    .flatMap(maxAllowed -> {
                        UnitDistributionValidator.validateMaxDistributionLimit(codesToCreate.size(), maxAllowed);
                
                // Buscar códigos que ya existen para creación parcial
                return unitRepository.findByOrganizationIdAndCodeIn(distribution.getOrganizationId(), codesToCreate)
                    .map(Unit::getCode)
                    .collectList()
                    .flatMap(existingCodes -> {
                        // Generar lista de rechazados
                        List<RejectedUnit> rejectedUnits = existingCodes.stream()
                            .map(RejectedUnit::duplicate)
                            .toList();
                        
                        // Filtrar unidades que sí se pueden crear (no duplicadas)
                        List<Unit> allUnits = buildUnits(distribution);
                        List<Unit> unitsToCreate = allUnits.stream()
                            .filter(u -> !existingCodes.contains(u.getCode()))
                            .toList();
                        
                        // Si no hay unidades nuevas que crear, retornar solo rechazados
                        if (unitsToCreate.isEmpty()) {
                            LOGGER.log(System.Logger.Level.INFO, 
                                "Todas las unidades ya existen ({0} rechazadas)", rejectedUnits.size());
                            return Mono.just(new UnitDistributionResult(
                                Collections.emptyList(), rejectedUnits
                            ));
                        }
                        
                        // Crear solo las unidades nuevas
                        return unitRepository.saveAll(unitsToCreate)
                            .collectList()
                            .flatMap(savedUnits -> {
                                LOGGER.log(System.Logger.Level.INFO, 
                                    "Unidades creadas: {0}, rechazadas: {1}", 
                                    savedUnits.size(), rejectedUnits.size());
                                
                                UnitDistributionResult result = new UnitDistributionResult(
                                    savedUnits, rejectedUnits
                                );
                                
                                // Si hay owner, procesar
                                if (distribution.getOwner() != null) {
                                    return processOwner(distribution, savedUnits, createdBy)
                                        .thenReturn(result);
                                }
                                
                                return Mono.just(result);
                            });
                    });
                    });
            });
    }
    
    /**
     * Genera los códigos de unidad según la distribución.
     */
    private List<String> generateCodes(UnitDistribution distribution) {
        return IntStream.rangeClosed(distribution.getMin(), distribution.getMax())
            .mapToObj(distribution::generateUnitCode)
            .toList();
    }
    
    /**
     * Construye los objetos Unit a partir de la distribución.
     */
    private List<Unit> buildUnits(UnitDistribution distribution) {
        List<Unit> units = new ArrayList<>();
        
        Boolean vehiclesEnabled = distribution.getVehiclesEnabled() != null 
            ? distribution.getVehiclesEnabled() 
            : Boolean.FALSE;
        
        Integer maxVehicles = Boolean.TRUE.equals(vehiclesEnabled) 
            ? distribution.getVehicleLimit() 
            : 0;
        
        for (int i = distribution.getMin(); i <= distribution.getMax(); i++) {
            Unit unit = Unit.builder()
                .organizationId(distribution.getOrganizationId())
                .zoneId(distribution.getZoneId())
                .towerId(distribution.getTowerId())
                .code(distribution.generateUnitCode(i))
                .type(distribution.getType())
                .floor(distribution.getFloor())
                .vehiclesEnabled(vehiclesEnabled)
                .maxVehicles(maxVehicles)
                .status(UnitStatus.AVAILABLE)
                .isActive(true)
                .build();
            
            units.add(unit);
        }
        
        return units;
    }
    
    /**
     * Procesa la asignación del propietario a las unidades.
     */
    private Mono<Void> processOwner(UnitDistribution distribution, List<Unit> units, Long createdBy) {
        OwnerInfo owner = distribution.getOwner();
        
        // Validar documento del propietario
        DocumentType docType = UserIdentificationValidator.validateComplete(
            owner.getDocumentType().name(), 
            owner.getDocumentNumber()
        );
        
        String normalizedDocNumber = docType.normalize(owner.getDocumentNumber());
        
        // Buscar o crear usuario
        return findOrCreateUser(owner, docType, normalizedDocNumber)
            .flatMap(user -> {
                // Crear asociaciones user-organization y user-unit
                return createAssociations(user, distribution.getOrganizationId(), units, createdBy)
                    .then(Mono.defer(() -> {
                        // Si sendInvitationImmediately, crear y enviar invitaciones
                        if (Boolean.TRUE.equals(distribution.getSendInvitationImmediately())) {
                            return createAndSendOwnerInvitations(
                                user, distribution.getOrganizationId(), units, createdBy
                            );
                        }
                        return Mono.empty();
                    }));
            });
    }
    
    /**
     * Busca un usuario existente o crea uno nuevo.
     */
    private Mono<AuthUser> findOrCreateUser(OwnerInfo owner, DocumentType docType, String normalizedDocNumber) {
        // Primero buscar por documento
        return authUserRepository.findByDocumentTypeAndNumber(docType.name(), normalizedDocNumber)
            .switchIfEmpty(
                // Si no existe por documento, buscar por email
                authUserRepository.findByEmail(owner.getEmail())
                    .switchIfEmpty(
                        // Si no existe, crear nuevo usuario
                        createPreRegisteredUser(owner, docType, normalizedDocNumber)
                    )
            )
            .flatMap(user -> {
                // Si encontramos por email pero sin documento, actualizar
                if (user.getDocumentType() == null && docType != null) {
                    AuthUser updated = user.toBuilder()
                        .documentType(docType)
                        .documentNumber(normalizedDocNumber)
                        .build();
                    return authUserRepository.save(updated);
                }
                return Mono.just(user);
            });
    }
    
    /**
     * Crea un usuario pre-registrado.
     */
    private Mono<AuthUser> createPreRegisteredUser(OwnerInfo owner, DocumentType docType, String normalizedDocNumber) {
        AuthUser newUser = AuthUser.builder()
            .names(owner.getNames() != null ? owner.getNames() : "Propietario")
            .email(owner.getEmail())
            .phone(owner.getPhone())
            .documentType(docType)
            .documentNumber(normalizedDocNumber)
            .status(UserStatus.PRE_REGISTERED)
            .active(false)
            .build();
        
        return authUserRepository.save(newUser)
            .doOnSuccess(u -> LOGGER.log(System.Logger.Level.INFO, 
                "Usuario pre-registrado creado: id={0}, email={1}", u.getId(), u.getEmail()));
    }
    
    /**
     * Crea asociaciones del usuario con la organización y las unidades.
     */
    private Mono<Void> createAssociations(AuthUser user, Long organizationId, List<Unit> units, Long createdBy) {
        // Crear user_organizations si no existe
        Mono<Void> orgAssociation = userOrganizationRepository
            .existsByUserIdAndOrganizationId(user.getId(), organizationId)
            .flatMap(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    return Mono.empty();
                }
                UserOrganization userOrg = UserOrganization.builder()
                    .userId(user.getId())
                    .organizationId(organizationId)
                    .status("ACTIVE")
                    .joinedAt(Instant.now())
                    .build();
                return userOrganizationRepository.save(userOrg).then();
            });
        
        // Obtener rol OWNER
        Mono<Long> ownerRoleId = roleRepository.findByCode(OWNER_ROLE_CODE)
            .switchIfEmpty(Mono.error(new NotFoundException("Role", OWNER_ROLE_CODE)))
            .map(role -> role.getId());
        
        return orgAssociation
            .then(ownerRoleId)
            .flatMap(roleId -> {
                // Crear user_units para cada unidad
                List<Mono<Void>> unitAssociations = units.stream()
                    .map(unit -> createUserUnitAssociation(user.getId(), unit.getId(), roleId, createdBy))
                    .collect(Collectors.toList());
                
                return Flux.merge(unitAssociations).then();
            });
    }
    
    /**
     * Crea asociación user-unit individual.
     */
    private Mono<Void> createUserUnitAssociation(Long userId, Long unitId, Long roleId, Long createdBy) {
        UserUnit userUnit = UserUnit.builder()
            .userId(userId)
            .unitId(unitId)
            .roleId(roleId)
            .ownershipType(OwnershipType.OWNER)
            .isPrimary(true)
            .status("PENDING") // Pendiente hasta aceptar invitación
            .invitedBy(createdBy)
            .joinedAt(Instant.now())
            .build();
        
        return userUnitRepository.save(userUnit).then();
    }
    
    /**
     * Crea y envía invitaciones para el propietario.
     */
    private Mono<Void> createAndSendOwnerInvitations(
            AuthUser user, Long organizationId, List<Unit> units, Long createdBy) {
        
        return roleRepository.findByCode(OWNER_ROLE_CODE)
            .switchIfEmpty(Mono.error(new NotFoundException("Role", OWNER_ROLE_CODE)))
            .flatMap(ownerRole -> {
                // Crear una invitación por cada unidad
                List<Mono<Invitation>> invitationMonos = units.stream()
                    .map(unit -> createOwnerInvitation(
                        organizationId, unit.getId(), user.getEmail(), 
                        ownerRole.getId(), createdBy
                    ))
                    .collect(Collectors.toList());
                
                return Flux.merge(invitationMonos)
                    .doOnNext(inv -> LOGGER.log(System.Logger.Level.INFO, 
                        "Invitación de propietario creada: id={0}, unitId={1}", 
                        inv.getId(), inv.getUnitId()))
                    .then();
            });
    }
    
    /**
     * Crea una invitación de propietario individual.
     */
    private Mono<Invitation> createOwnerInvitation(
            Long organizationId, Long unitId, String email, Long roleId, Long createdBy) {
        
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS);
        
        Invitation invitation = Invitation.builder()
            .organizationId(organizationId)
            .unitId(unitId)
            .email(email)
            .invitationToken(token)
            .type(InvitationType.OWNER_INVITATION)
            .roleId(roleId)
            .status(InvitationStatus.PENDING)
            .invitedBy(createdBy)
            .expiresAt(expiresAt)
            .invitationMailStatus("PENDING")
            .retryCount(0)
            .build();
        
        return invitationRepository.save(invitation)
            .flatMap(savedInvitation -> {
                // Registrar en auditoría
                return invitationAuditRepository.logAction(
                    savedInvitation.getId(), 
                    InvitationAuditRepository.ACTION_CREATED, 
                    createdBy
                ).then(Mono.just(savedInvitation));
            })
            .flatMap(savedInvitation -> {
                // Enviar email
                return sendOwnerInvitationEmail(savedInvitation)
                    .then(Mono.just(savedInvitation));
            });
    }
    
    /**
     * Envía email de invitación al propietario.
     */
    private Mono<Void> sendOwnerInvitationEmail(Invitation invitation) {
        String activationUrl = frontendUrl + "/owner/activate/" + invitation.getInvitationToken();
        
        return notificationGateway.sendOwnerInvitationEmail(
                invitation.getEmail(),
                invitation.getInvitationToken(),
                activationUrl,
                invitation
            )
            .flatMap(v -> invitationRepository.updateMailStatus(
                invitation.getId(), "SENT", Instant.now()
            ))
            .then(invitationAuditRepository.logAction(
                invitation.getId(), 
                InvitationAuditRepository.ACTION_SENT, 
                null
            ))
            .doOnError(e -> {
                LOGGER.log(System.Logger.Level.ERROR, 
                    "Error enviando email de invitación: {0}", e.getMessage());
                // Marcar como fallido
                invitationRepository.updateMailStatus(invitation.getId(), "FAILED", null)
                    .then(invitationAuditRepository.logAction(
                        invitation.getId(), 
                        InvitationAuditRepository.ACTION_FAILED, 
                        null
                    ))
                    .subscribe();
            });
    }
}
