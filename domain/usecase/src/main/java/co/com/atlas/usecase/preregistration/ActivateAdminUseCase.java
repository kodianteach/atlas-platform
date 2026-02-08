package co.com.atlas.usecase.preregistration;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.preregistration.AdminActivationToken;
import co.com.atlas.model.preregistration.ActivationTokenStatus;
import co.com.atlas.model.preregistration.PreRegistrationAuditAction;
import co.com.atlas.model.preregistration.PreRegistrationAuditLog;
import co.com.atlas.model.preregistration.gateways.AdminActivationTokenRepository;
import co.com.atlas.model.preregistration.gateways.PreRegistrationAuditRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Caso de uso para activación de administradores pre-registrados.
 * 
 * Este caso de uso valida el token de activación y las credenciales temporales,
 * y marca al usuario como ACTIVATED para que pueda crear su company/organization.
 */
@RequiredArgsConstructor
public class ActivateAdminUseCase {
    
    private final AuthUserRepository authUserRepository;
    private final AdminActivationTokenRepository tokenRepository;
    private final PreRegistrationAuditRepository auditRepository;
    private final NotificationGateway notificationGateway;
    
    /**
     * Comando para activación de administrador.
     */
    public record ActivateCommand(
            String token,
            String email,
            String password,
            String newPassword,
            String clientIp,
            String clientUserAgent
    ) {}
    
    /**
     * Resultado de la activación.
     */
    public record ActivateResult(
            Long userId,
            String email,
            String names,
            UserStatus status,
            String message
    ) {}
    
    /**
     * Valida el token de activación (sin consumirlo).
     * Útil para mostrar el formulario de activación.
     * 
     * @param rawToken Token en claro
     * @return Información básica del token si es válido
     */
    public Mono<TokenValidationResult> validateToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        
        return tokenRepository.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new NotFoundException("Token inválido o no encontrado")))
                .flatMap(token -> {
                    if (token.getStatus() == ActivationTokenStatus.CONSUMED) {
                        return Mono.error(new BusinessException(
                                "Este token ya fue utilizado", "TOKEN_ALREADY_USED"));
                    }
                    if (token.getStatus() == ActivationTokenStatus.REVOKED) {
                        return Mono.error(new BusinessException(
                                "Este token fue revocado", "TOKEN_REVOKED"));
                    }
                    if (token.isExpired()) {
                        // Marcar como expirado si aún no lo está
                        return markTokenExpired(token)
                                .then(Mono.error(new BusinessException(
                                        "Este token ha expirado", "TOKEN_EXPIRED")));
                    }
                    
                    return authUserRepository.findById(token.getUserId())
                            .map(user -> new TokenValidationResult(
                                    token.getUserId(),
                                    user.getEmail(),
                                    user.getNames(),
                                    token.getExpiresAt(),
                                    true
                            ));
                });
    }
    
    /**
     * Resultado de validación de token.
     */
    public record TokenValidationResult(
            Long userId,
            String email,
            String names,
            Instant expiresAt,
            boolean valid
    ) {}
    
    /**
     * Ejecuta la activación del administrador.
     * 
     * @param command Datos de activación
     * @return Resultado de la activación
     */
    public Mono<ActivateResult> execute(ActivateCommand command) {
        String tokenHash = hashToken(command.token());
        
        return tokenRepository.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new NotFoundException("Token inválido o no encontrado")))
                .flatMap(token -> validateTokenForActivation(token, command))
                .flatMap(token -> activateUser(token, command));
    }
    
    private Mono<AdminActivationToken> validateTokenForActivation(AdminActivationToken token, 
                                                                   ActivateCommand command) {
        // Validar estado del token
        if (token.getStatus() == ActivationTokenStatus.CONSUMED) {
            return Mono.error(new BusinessException(
                    "Este token ya fue utilizado", "TOKEN_ALREADY_USED"));
        }
        if (token.getStatus() == ActivationTokenStatus.REVOKED) {
            return Mono.error(new BusinessException(
                    "Este token fue revocado", "TOKEN_REVOKED"));
        }
        if (token.isExpired()) {
            return markTokenExpired(token)
                    .then(Mono.error(new BusinessException(
                            "Este token ha expirado", "TOKEN_EXPIRED")));
        }
        
        // Validar credenciales
        return authUserRepository.findById(token.getUserId())
                .switchIfEmpty(Mono.error(new NotFoundException("Usuario no encontrado")))
                .flatMap(user -> {
                    // Validar email
                    if (!user.getEmail().equalsIgnoreCase(command.email())) {
                        return Mono.error(new BusinessException(
                                "Credenciales inválidas", "INVALID_CREDENTIALS"));
                    }
                    
                    // Validar contraseña temporal
                    return authUserRepository.validatePassword(command.password(), user.getPasswordHash())
                            .flatMap(valid -> {
                                if (!Boolean.TRUE.equals(valid)) {
                                    return Mono.error(new BusinessException(
                                            "Credenciales inválidas", "INVALID_CREDENTIALS"));
                                }
                                return Mono.just(token);
                            });
                });
    }
    
    private Mono<ActivateResult> activateUser(AdminActivationToken token, ActivateCommand command) {
        return authUserRepository.findById(token.getUserId())
                .flatMap(user -> {
                    // Actualizar usuario con nueva contraseña y estado ACTIVATED
                    AuthUser updatedUser = user.toBuilder()
                            .passwordHash(command.newPassword()) // El adapter hará el hash
                            .status(UserStatus.ACTIVATED)
                            .active(true)
                            .build();
                    
                    return authUserRepository.save(updatedUser);
                })
                .flatMap(updatedUser -> {
                    // Marcar token como consumido
                    AdminActivationToken consumedToken = token.toBuilder()
                            .status(ActivationTokenStatus.CONSUMED)
                            .consumedAt(Instant.now())
                            .activationIp(command.clientIp())
                            .activationUserAgent(command.clientUserAgent())
                            .build();
                    
                    return tokenRepository.save(consumedToken)
                            .thenReturn(updatedUser);
                })
                .flatMap(updatedUser -> 
                    auditRepository.save(PreRegistrationAuditLog.builder()
                            .tokenId(token.getId())
                            .action(PreRegistrationAuditAction.ACTIVATED)
                            .ipAddress(command.clientIp())
                            .userAgent(command.clientUserAgent())
                            .build())
                    .thenReturn(updatedUser)
                )
                .flatMap(updatedUser ->
                    notificationGateway.sendActivationConfirmationEmail(
                            updatedUser.getEmail(),
                            updatedUser.getNames()
                    )
                    .thenReturn(updatedUser)
                )
                .map(user -> new ActivateResult(
                        user.getId(),
                        user.getEmail(),
                        user.getNames(),
                        user.getStatus(),
                        "Cuenta activada exitosamente. Ya puede crear su compañía y organización."
                ));
    }
    
    private Mono<AdminActivationToken> markTokenExpired(AdminActivationToken token) {
        AdminActivationToken expiredToken = token.toBuilder()
                .status(ActivationTokenStatus.EXPIRED)
                .build();
        
        return tokenRepository.save(expiredToken)
                .flatMap(saved -> 
                    auditRepository.save(PreRegistrationAuditLog.builder()
                            .tokenId(saved.getId())
                            .action(PreRegistrationAuditAction.EXPIRED)
                            .build())
                    .thenReturn(saved)
                );
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
