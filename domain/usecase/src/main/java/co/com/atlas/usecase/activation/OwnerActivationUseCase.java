package co.com.atlas.usecase.activation;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.invitation.gateways.InvitationAuditRepository;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.userorganization.UserOrganization;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userrolemulti.UserRoleMulti;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Caso de uso para activación de cuenta de propietario.
 * 
 * Flujo:
 * 1. Validar token
 * 2. Crear contraseña
 * 3. Activar usuario
 * 4. Asignar rol OWNER
 * 5. Actualizar asociaciones
 */
@RequiredArgsConstructor
public class OwnerActivationUseCase {
    
    private static final System.Logger LOGGER = System.getLogger(OwnerActivationUseCase.class.getName());
    
    private final InvitationRepository invitationRepository;
    private final InvitationAuditRepository invitationAuditRepository;
    private final AuthUserRepository authUserRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserUnitRepository userUnitRepository;
    private final UserRoleMultiRepository userRoleMultiRepository;
    private final NotificationGateway notificationGateway;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Interface funcional para codificación de contraseña.
     */
    public interface PasswordEncoder {
        String encode(String rawPassword);
    }
    
    /**
     * Valida un token de invitación de propietario.
     * 
     * @param token token de la invitación
     * @return información de la invitación si es válida
     */
    public Mono<InvitationValidationResult> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new BusinessException("El token es requerido", "TOKEN_REQUIRED"));
        }
        
        return invitationRepository.findByToken(token)
            .switchIfEmpty(Mono.error(new NotFoundException("Invitación no encontrada")))
            .flatMap(invitation -> {
                // Validar tipo
                if (invitation.getType() != InvitationType.OWNER_INVITATION) {
                    return Mono.error(new BusinessException(
                        "Este token no corresponde a una invitación de propietario",
                        "INVALID_INVITATION_TYPE"
                    ));
                }
                
                // Validar estado
                if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
                    return Mono.error(new BusinessException(
                        "Esta invitación ya fue aceptada",
                        "INVITATION_ALREADY_ACCEPTED"
                    ));
                }
                
                if (invitation.getStatus() == InvitationStatus.CANCELLED) {
                    return Mono.error(new BusinessException(
                        "Esta invitación ha sido cancelada",
                        "INVITATION_CANCELLED"
                    ));
                }
                
                // Validar expiración
                if (invitation.getExpiresAt().isBefore(Instant.now())) {
                    // Marcar como expirada
                    Invitation expired = invitation.toBuilder()
                        .status(InvitationStatus.EXPIRED)
                        .build();
                    return invitationRepository.save(expired)
                        .then(Mono.error(new BusinessException(
                            "Esta invitación ha expirado",
                            "INVITATION_EXPIRED"
                        )));
                }
                
                // Token válido
                return Mono.just(InvitationValidationResult.builder()
                    .valid(true)
                    .invitation(invitation)
                    .email(invitation.getEmail())
                    .organizationId(invitation.getOrganizationId())
                    .unitId(invitation.getUnitId())
                    .expiresAt(invitation.getExpiresAt())
                    .invitationId(invitation.getId())
                    .names(null) // Se obtiene del user si existe
                    .organizationName(null) // Requiere lookup adicional
                    .unitCode(null) // Requiere lookup adicional
                    .userExists(false) // Se debe verificar
                    .message("Token válido")
                    .errorCode(null)
                    .build());
            });
    }
    
    /**
     * Completa la activación del propietario.
     * 
     * @param token token de la invitación
     * @param password contraseña elegida por el usuario
     * @param confirmPassword confirmación de contraseña
     * @return usuario activado
     */
    public Mono<AuthUser> completeActivation(String token, String password, String confirmPassword) {
        // Validar contraseñas
        if (password == null || password.length() < 8) {
            return Mono.error(new BusinessException(
                "La contraseña debe tener al menos 8 caracteres",
                "PASSWORD_TOO_SHORT"
            ));
        }
        
        if (!password.equals(confirmPassword)) {
            return Mono.error(new BusinessException(
                "Las contraseñas no coinciden",
                "PASSWORD_MISMATCH"
            ));
        }
        
        // Validar requisitos de contraseña
        if (!isPasswordStrong(password)) {
            return Mono.error(new BusinessException(
                "La contraseña debe contener al menos una mayúscula, una minúscula y un número",
                "PASSWORD_WEAK"
            ));
        }
        
        return validateToken(token)
            .flatMap(validation -> {
                Invitation invitation = validation.getInvitation();
                
                // Buscar usuario por email
                return authUserRepository.findByEmail(invitation.getEmail())
                    .switchIfEmpty(Mono.error(new NotFoundException("Usuario no encontrado")))
                    .flatMap(user -> {
                        // Verificar que el usuario no esté ya activo
                        if (user.getStatus() == UserStatus.ACTIVE) {
                            return Mono.error(new BusinessException(
                                "Este usuario ya tiene una cuenta activa",
                                "USER_ALREADY_ACTIVE"
                            ));
                        }
                        
                        return activateUser(user, password, invitation);
                    });
            });
    }
    
    /**
     * Activa el usuario y configura sus asociaciones.
     */
    private Mono<AuthUser> activateUser(AuthUser user, String password, Invitation invitation) {
        String hashedPassword = passwordEncoder.encode(password);
        
        // Actualizar usuario
        AuthUser activatedUser = user.toBuilder()
            .passwordHash(hashedPassword)
            .status(UserStatus.ACTIVE)
            .active(true)
            .build();
        
        return authUserRepository.save(activatedUser)
            .flatMap(savedUser -> {
                // Actualizar invitación
                Invitation accepted = invitation.toBuilder()
                    .status(InvitationStatus.ACCEPTED)
                    .acceptedAt(Instant.now())
                    .build();
                
                return invitationRepository.save(accepted)
                    .then(invitationAuditRepository.logAction(
                        invitation.getId(),
                        InvitationAuditRepository.ACTION_ACCEPTED,
                        savedUser.getId()
                    ))
                    .then(assignOwnerRole(savedUser, invitation))
                    .then(updateUserUnitStatus(savedUser.getId(), invitation.getUnitId()))
                    .then(updateUserOrganizationStatus(savedUser.getId(), invitation.getOrganizationId()))
                    .then(sendConfirmationEmail(savedUser))
                    .thenReturn(savedUser);
            })
            .doOnSuccess(u -> LOGGER.log(System.Logger.Level.INFO, 
                "Usuario activado exitosamente: id={0}, email={1}", u.getId(), u.getEmail()));
    }
    
    /**
     * Asigna el rol OWNER al usuario en la organización.
     */
    private Mono<Void> assignOwnerRole(AuthUser user, Invitation invitation) {
        if (invitation.getRoleId() == null) {
            LOGGER.log(System.Logger.Level.WARNING, 
                "Invitación sin roleId definido: {0}", invitation.getId());
            return Mono.empty();
        }
        
        UserRoleMulti userRole = UserRoleMulti.builder()
            .userId(user.getId())
            .organizationId(invitation.getOrganizationId())
            .roleId(invitation.getRoleId())
            .isPrimary(true)
            .assignedAt(Instant.now())
            .build();
        
        return userRoleMultiRepository.save(userRole)
            .doOnSuccess(r -> LOGGER.log(System.Logger.Level.INFO, 
                "Rol OWNER asignado: userId={0}, orgId={1}, roleId={2}", 
                user.getId(), invitation.getOrganizationId(), invitation.getRoleId()))
            .then();
    }
    
    /**
     * Actualiza el estado de user_units a ACTIVE.
     */
    private Mono<Void> updateUserUnitStatus(Long userId, Long unitId) {
        if (unitId == null) {
            return Mono.empty();
        }
        return userUnitRepository.updateStatusByUserIdAndUnitId(userId, unitId, "ACTIVE");
    }
    
    /**
     * Actualiza el estado de user_organizations.
     */
    private Mono<Void> updateUserOrganizationStatus(Long userId, Long organizationId) {
        return userOrganizationRepository.updateStatusByUserIdAndOrganizationId(
            userId, organizationId, "ACTIVE"
        );
    }
    
    /**
     * Envía email de confirmación.
     */
    private Mono<Void> sendConfirmationEmail(AuthUser user) {
        return notificationGateway.sendActivationConfirmationEmail(user.getEmail(), user.getNames())
            .doOnError(e -> LOGGER.log(System.Logger.Level.WARNING, 
                "Error enviando email de confirmación: {0}", e.getMessage()))
            .onErrorResume(e -> Mono.empty()); // No fallar si el email falla
    }
    
    /**
     * Valida que la contraseña sea fuerte.
     */
    private boolean isPasswordStrong(String password) {
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasUpper && hasLower && hasDigit;
    }
    
    /**
     * Resultado de validación de invitación.
     */
    @lombok.Builder
    @lombok.Getter
    public static class InvitationValidationResult {
        private boolean valid;
        private Invitation invitation;
        private String email;
        private Long organizationId;
        private Long unitId;
        private Instant expiresAt;
        
        // Campos adicionales para la respuesta del handler
        private String names;
        private String organizationName;
        private String unitCode;
        private Long invitationId;
        private boolean userExists;
        private String message;
        private String errorCode;
    }
}
