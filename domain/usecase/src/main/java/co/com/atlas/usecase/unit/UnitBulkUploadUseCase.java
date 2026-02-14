package co.com.atlas.usecase.unit;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.auth.validation.UserIdentificationValidator;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.invitation.gateways.InvitationAuditRepository;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.unit.BulkUnitRow;
import co.com.atlas.model.unit.BulkUploadResult;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.UnitStatus;
import co.com.atlas.model.unit.UnitType;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.unit.validation.BulkUploadValidator;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para carga masiva de unidades desde Excel/CSV.
 * 
 * Flujo de dos pasos:
 * 1. validate() - Procesa el archivo y retorna resultado de validación
 * 2. processBulk() - Procesa las filas válidas confirmadas por el admin
 */
@RequiredArgsConstructor
public class UnitBulkUploadUseCase {
    
    private static final System.Logger LOGGER = System.getLogger(UnitBulkUploadUseCase.class.getName());
    
    private final UnitRepository unitRepository;
    private final OrganizationRepository organizationRepository;
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
     * Valida los datos parseados del archivo de carga masiva.
     * Este método no persiste nada, solo valida y retorna el resultado.
     * 
     * @param rows filas parseadas del archivo
     * @param organizationId ID de la organización destino
     * @return resultado de validación con filas válidas y errores
     */
    public Mono<BulkUploadResult> validate(List<BulkUnitRow> rows, Long organizationId) {
        if (rows == null || rows.isEmpty()) {
            return Mono.just(BulkUploadResult.builder()
                .validRows(List.of())
                .errorRows(List.of())
                .totalRows(0)
                .validCount(0)
                .errorCount(0)
                .hasCriticalErrors(true)
                .message("El archivo no contiene filas de datos")
                .structuralErrors(List.of("Archivo vacío"))
                .build());
        }
        
        LOGGER.log(System.Logger.Level.INFO, 
            "Validando {0} filas para organización {1}", rows.size(), organizationId);
        
        return organizationRepository.findById(organizationId)
            .switchIfEmpty(Mono.error(new NotFoundException("Organization", organizationId)))
            .flatMap(org -> {
                // Paso 1: Validar cada fila individualmente
                List<BulkUnitRow> validatedRows = rows.stream()
                    .map(BulkUploadValidator::validateRow)
                    .collect(Collectors.toList());
                
                // Paso 2: Validar duplicados dentro del lote
                validatedRows = BulkUploadValidator.validateDuplicatesInBatch(validatedRows);
                
                // Paso 3: Validar contra BD (códigos existentes, usuarios existentes)
                return validateAgainstDatabase(validatedRows, organizationId)
                    .map(finalRows -> buildValidationResult(finalRows));
            });
    }
    
    /**
     * Valida las filas contra la base de datos.
     */
    private Mono<List<BulkUnitRow>> validateAgainstDatabase(List<BulkUnitRow> rows, Long organizationId) {
        // Extraer códigos de unidad generados
        List<String> unitCodes = rows.stream()
            .filter(r -> r.getGeneratedCode() != null)
            .map(BulkUnitRow::getGeneratedCode)
            .distinct()
            .toList();
        
        // Verificar códigos existentes en BD
        return unitRepository.findByOrganizationIdAndCodeIn(organizationId, unitCodes)
            .collectList()
            .flatMap(existingUnits -> {
                // Marcar filas con códigos que ya existen
                var existingCodes = existingUnits.stream()
                    .map(Unit::getCode)
                    .collect(Collectors.toSet());
                
                rows.forEach(row -> {
                    if (row.getGeneratedCode() != null && existingCodes.contains(row.getGeneratedCode())) {
                        List<String> errors = new ArrayList<>(row.getErrors() != null ? row.getErrors() : List.of());
                        errors.add("Ya existe una unidad con código: " + row.getGeneratedCode());
                        row.setErrors(errors);
                        row.setValid(false);
                    }
                });
                
                // Verificar documentos existentes
                return validateDocumentsInDatabase(rows);
            });
    }
    
    /**
     * Valida documentos de propietarios contra la BD.
     */
    private Mono<List<BulkUnitRow>> validateDocumentsInDatabase(List<BulkUnitRow> rows) {
        // Para cada fila válida, verificar si el usuario ya existe
        List<Mono<BulkUnitRow>> validations = rows.stream()
            .map(row -> {
                if (!row.getValid() || row.getDocumentType() == null || row.getDocumentNumber() == null) {
                    return Mono.just(row);
                }
                
                String docType = row.getDocumentType().name();
                String docNumber = row.getDocumentType().normalize(row.getDocumentNumber());
                
                return authUserRepository.findByDocumentTypeAndNumber(docType, docNumber)
                    .map(existingUser -> {
                        // Usuario existe - agregar warning pero no es error
                        List<String> warnings = new ArrayList<>(row.getWarnings() != null ? row.getWarnings() : List.of());
                        warnings.add("Usuario ya existe con este documento (ID: " + existingUser.getId() + ")");
                        row.setWarnings(warnings);
                        return row;
                    })
                    .defaultIfEmpty(row);
            })
            .toList();
        
        return Flux.merge(validations).collectList();
    }
    
    /**
     * Construye el resultado de validación.
     */
    private BulkUploadResult buildValidationResult(List<BulkUnitRow> rows) {
        List<BulkUnitRow> validRows = rows.stream()
            .filter(BulkUnitRow::getValid)
            .toList();
        
        List<BulkUnitRow> errorRows = rows.stream()
            .filter(r -> !r.getValid())
            .toList();
        
        boolean hasCriticalErrors = errorRows.size() == rows.size(); // Todas las filas tienen errores
        
        String message;
        if (hasCriticalErrors) {
            message = "Todas las filas contienen errores. Corrija los errores y vuelva a intentar.";
        } else if (errorRows.isEmpty()) {
            message = "Validación exitosa. Todas las filas son válidas.";
        } else {
            message = String.format("Validación completada: %d filas válidas, %d filas con errores.", 
                validRows.size(), errorRows.size());
        }
        
        return BulkUploadResult.builder()
            .validRows(validRows)
            .errorRows(errorRows)
            .totalRows(rows.size())
            .validCount(validRows.size())
            .errorCount(errorRows.size())
            .hasCriticalErrors(hasCriticalErrors)
            .message(message)
            .structuralErrors(List.of())
            .build();
    }
    
    /**
     * Procesa las filas válidas confirmadas.
     * Este método persiste unidades, usuarios e invitaciones.
     * 
     * @param validRows filas validadas y confirmadas por el admin
     * @param organizationId ID de la organización destino
     * @param unitType tipo de unidad a crear (APARTMENT o HOUSE)
     * @param sendInvitations si se deben enviar invitaciones inmediatamente
     * @param createdBy ID del usuario que realiza la operación
     * @return lista de unidades creadas
     */
    public Flux<Unit> processBulk(
            List<BulkUnitRow> validRows, 
            Long organizationId,
            UnitType unitType,
            boolean sendInvitations, 
            Long createdBy) {
        
        if (validRows == null || validRows.isEmpty()) {
            return Flux.error(new BusinessException("No hay filas válidas para procesar", "NO_VALID_ROWS"));
        }
        
        LOGGER.log(System.Logger.Level.INFO, 
            "Procesando {0} filas para organización {1}, sendInvitations={2}", 
            validRows.size(), organizationId, sendInvitations);
        
        return organizationRepository.findById(organizationId)
            .switchIfEmpty(Mono.error(new NotFoundException("Organization", organizationId)))
            .flatMapMany(org -> {
                // Crear todas las unidades primero
                List<Unit> unitsToCreate = validRows.stream()
                    .map(row -> buildUnitFromRow(row, organizationId, unitType))
                    .toList();
                
                return unitRepository.saveAll(unitsToCreate)
                    .collectList()
                    .flatMapMany(savedUnits -> {
                        LOGGER.log(System.Logger.Level.INFO, 
                            "{0} unidades creadas exitosamente", savedUnits.size());
                        
                        // Procesar propietarios para cada unidad
                        return processOwnersForUnits(validRows, savedUnits, organizationId, sendInvitations, createdBy)
                            .thenMany(Flux.fromIterable(savedUnits));
                    });
            });
    }
    
    /**
     * Construye una Unit a partir de una fila del archivo.
     */
    private Unit buildUnitFromRow(BulkUnitRow row, Long organizationId, UnitType unitType) {
        Integer maxVehicles = row.getVehicleLimit() != null ? row.getVehicleLimit() : 0;
        Boolean vehiclesEnabled = maxVehicles > 0;
        
        return Unit.builder()
            .organizationId(organizationId)
            .code(row.getGeneratedCode())
            .type(unitType)
            .vehiclesEnabled(vehiclesEnabled)
            .maxVehicles(maxVehicles)
            .status(UnitStatus.AVAILABLE)
            .isActive(true)
            .build();
    }
    
    /**
     * Procesa propietarios para las unidades creadas.
     */
    private Mono<Void> processOwnersForUnits(
            List<BulkUnitRow> rows, 
            List<Unit> savedUnits,
            Long organizationId,
            boolean sendInvitations,
            Long createdBy) {
        
        // Mapear filas a unidades por código
        var rowByCode = rows.stream()
            .collect(Collectors.toMap(BulkUnitRow::getGeneratedCode, r -> r));
        
        List<Mono<Void>> ownerProcessing = savedUnits.stream()
            .map(unit -> {
                BulkUnitRow row = rowByCode.get(unit.getCode());
                if (row == null) {
                    return Mono.<Void>empty();
                }
                return processOwnerForUnit(row, unit, organizationId, sendInvitations, createdBy);
            })
            .toList();
        
        return Flux.merge(ownerProcessing).then();
    }
    
    /**
     * Procesa un propietario para una unidad individual.
     */
    private Mono<Void> processOwnerForUnit(
            BulkUnitRow row, 
            Unit unit, 
            Long organizationId,
            boolean sendInvitations,
            Long createdBy) {
        
        DocumentType docType = row.getDocumentType();
        String normalizedDocNumber = docType.normalize(row.getDocumentNumber());
        
        // Buscar o crear usuario
        return findOrCreateUser(row, docType, normalizedDocNumber)
            .flatMap(user -> {
                // Crear asociaciones
                return createAssociations(user, organizationId, unit, createdBy)
                    .then(Mono.defer(() -> {
                        if (sendInvitations) {
                            return createAndSendOwnerInvitation(
                                user, organizationId, unit.getId(), createdBy
                            );
                        }
                        return Mono.empty();
                    }));
            });
    }
    
    /**
     * Busca o crea un usuario.
     */
    private Mono<AuthUser> findOrCreateUser(BulkUnitRow row, DocumentType docType, String normalizedDocNumber) {
        return authUserRepository.findByDocumentTypeAndNumber(docType.name(), normalizedDocNumber)
            .switchIfEmpty(
                authUserRepository.findByEmail(row.getOwnerEmail())
                    .switchIfEmpty(createPreRegisteredUser(row, docType, normalizedDocNumber))
            )
            .flatMap(user -> {
                // Actualizar documento si falta
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
    private Mono<AuthUser> createPreRegisteredUser(BulkUnitRow row, DocumentType docType, String normalizedDocNumber) {
        AuthUser newUser = AuthUser.builder()
            .names("Propietario")
            .email(row.getOwnerEmail())
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
     * Crea asociaciones user-organization y user-unit.
     */
    private Mono<Void> createAssociations(AuthUser user, Long organizationId, Unit unit, Long createdBy) {
        // User-Organization
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
        
        // User-Unit
        return orgAssociation
            .then(roleRepository.findByCode(OWNER_ROLE_CODE))
            .switchIfEmpty(Mono.error(new NotFoundException("Role", OWNER_ROLE_CODE)))
            .flatMap(ownerRole -> {
                UserUnit userUnit = UserUnit.builder()
                    .userId(user.getId())
                    .unitId(unit.getId())
                    .roleId(ownerRole.getId())
                    .ownershipType(OwnershipType.OWNER)
                    .isPrimary(true)
                    .status("PENDING")
                    .invitedBy(createdBy)
                    .joinedAt(Instant.now())
                    .build();
                return userUnitRepository.save(userUnit).then();
            });
    }
    
    /**
     * Crea y envía invitación de propietario.
     */
    private Mono<Void> createAndSendOwnerInvitation(
            AuthUser user, Long organizationId, Long unitId, Long createdBy) {
        
        return roleRepository.findByCode(OWNER_ROLE_CODE)
            .switchIfEmpty(Mono.error(new NotFoundException("Role", OWNER_ROLE_CODE)))
            .flatMap(ownerRole -> {
                String token = UUID.randomUUID().toString();
                Instant expiresAt = Instant.now().plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS);
                
                Invitation invitation = Invitation.builder()
                    .organizationId(organizationId)
                    .unitId(unitId)
                    .email(user.getEmail())
                    .invitationToken(token)
                    .type(InvitationType.OWNER_INVITATION)
                    .roleId(ownerRole.getId())
                    .status(InvitationStatus.PENDING)
                    .invitedBy(createdBy)
                    .expiresAt(expiresAt)
                    .invitationMailStatus("PENDING")
                    .retryCount(0)
                    .build();
                
                return invitationRepository.save(invitation)
                    .flatMap(savedInvitation -> {
                        return invitationAuditRepository.logAction(
                            savedInvitation.getId(),
                            InvitationAuditRepository.ACTION_CREATED,
                            createdBy
                        ).then(Mono.just(savedInvitation));
                    })
                    .flatMap(this::sendOwnerInvitationEmail)
                    .then();
            });
    }
    
    /**
     * Envía email de invitación.
     */
    private Mono<Invitation> sendOwnerInvitationEmail(Invitation invitation) {
        String activationUrl = frontendUrl + "/owner/activate?token=" + invitation.getInvitationToken();
        
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
            .thenReturn(invitation)
            .onErrorResume(e -> {
                LOGGER.log(System.Logger.Level.ERROR, 
                    "Error enviando email de invitación: {0}", e.getMessage());
                return invitationRepository.updateMailStatus(invitation.getId(), "FAILED", null)
                    .then(invitationAuditRepository.logAction(
                        invitation.getId(),
                        InvitationAuditRepository.ACTION_FAILED,
                        null
                    ))
                    .thenReturn(invitation);
            });
    }
}
